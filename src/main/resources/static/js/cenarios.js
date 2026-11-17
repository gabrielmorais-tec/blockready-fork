/* Emergency Scenario: simulação de cenários de 72h com estratégia de priorização (Etapa 4). */
BR.ready.then(async (me) => {
  if (!me || !BR.perfilStaff(me)) { return; }

  const corSituacao = { SUFICIENTE: 'var(--ok)', PARCIAL: 'var(--alert)', INSUFICIENTE: 'var(--danger)', SEM_DADOS: 'var(--gray-300)' };
  document.getElementById('c-tipo').innerHTML = BR.options('tipoEmergencia');
  let ultimaRequisicao = null;

  function montarRequisicao() {
    const num = (id) => {
      const v = document.getElementById(id).value;
      return v === '' ? null : Number(v);
    };
    return {
      nome: document.getElementById('c-nome').value.trim(),
      tipo: document.getElementById('c-tipo').value,
      horizonteHoras: Number(document.getElementById('c-horizonte').value),
      percentualAfetados: Number(document.getElementById('c-afetados').value),
      parametros: {
        aguaLitrosPorPessoaDia: num('p-agua'),
        refeicoesPorPessoaDia: num('p-refeicoes'),
        percentualDesabrigados: num('p-desabrigados'),
        percentualEnergia: num('p-energia'),
        percentualComunicacao: num('p-comunicacao'),
        percentualSaude: num('p-saude')
      }
    };
  }

  function mostrarResultado(r) {
    document.getElementById('resultado').hidden = false;
    document.getElementById('resultado-info').textContent =
      `${BR.rotulo('tipoEmergencia', r.tipo)} · ${r.horizonteHoras} h · ${BR.num(r.percentualAfetados)}% da população ` +
      `(${BR.num(r.cri.populacaoAfetada)} de ${BR.num(r.cri.populacaoBase)} pessoas, IBGE) · estratégia ${r.estrategia}. ` +
      (r.avisos ? r.avisos[0] : '');

    document.getElementById('resultado-cards').innerHTML = r.cri.categorias.map((c) => `
      <section class="panel">
        <div style="display:flex;justify-content:space-between"><h3>${BR.esc(BR.rotulo('categoria', c.categoria))}</h3>${BR.badge('situacaoCobertura', c.situacao)}</div>
        <ul class="data-list">
          <li><span class="label">Disponível</span><span>${BR.num(c.capacidadeDisponivel)} ${BR.esc(c.unidade)}</span></li>
          <li><span class="label">Demanda no cenário</span><span>${BR.num(c.demandaEstimada)} ${BR.esc(c.unidade)}</span></li>
          <li><span class="label">Cobertura</span><span>${c.percentual == null ? '—' : BR.num(c.percentual) + '%'}</span></li>
          <li><span class="label">Déficit</span><span>${c.insuficiencia == null ? '—' : BR.num(c.insuficiencia) + ' ' + BR.esc(c.unidade)}</span></li>
        </ul>
        ${c.observacao ? `<p class="panel-note">${BR.esc(c.observacao)}</p>` : ''}
      </section>`).join('');

    document.getElementById('resultado-grafico').innerHTML = r.cri.categorias.map((c) => {
      const pct = c.percentual == null ? 0 : Math.min(Number(c.percentual), 100);
      const rotulo = c.percentual == null ? 'Sem dados' : BR.num(c.percentual) + '%';
      return `<div>
        <div style="display:flex;justify-content:space-between"><span>${BR.esc(BR.rotulo('categoria', c.categoria))}</span><span>${rotulo}</span></div>
        <div style="background:var(--gray-100);border-radius:999px;height:.75rem;overflow:hidden" role="img" aria-label="${BR.esc(BR.rotulo('categoria', c.categoria))}: ${rotulo}">
          <div style="width:${pct}%;height:100%;background:${corSituacao[c.situacao] || 'var(--blue)'}"></div>
        </div></div>`;
    }).join('');

    document.getElementById('resultado-recomendacoes').innerHTML = r.recomendacoes.length
      ? r.recomendacoes.map((rec) => `
          <tr>
            <td>${rec.prioridade}</td>
            <td>${BR.esc(BR.rotulo('categoria', rec.categoria))}</td>
            <td>${rec.cobertura == null ? '—' : BR.num(rec.cobertura) + '%'}</td>
            <td>${rec.deficit == null ? '—' : BR.num(rec.deficit) + ' ' + BR.esc(rec.unidade)}</td>
            <td>${BR.esc(rec.acao)}</td>
            <td class="hint">${BR.esc(rec.justificativa)}</td>
          </tr>`).join('')
      : '<tr><td colspan="6">Nenhuma mobilização prioritária identificada com os dados atuais.</td></tr>';
  }

  document.getElementById('form-cenario').addEventListener('submit', async (ev) => {
    ev.preventDefault();
    const erro = document.getElementById('erro-simular');
    BR.limparErro(erro);
    try {
      const req = montarRequisicao();
      const r = await BR.api.post('/cenarios/simular', req);
      ultimaRequisicao = req;
      document.getElementById('btn-salvar').disabled = false;
      mostrarResultado(r);
    } catch (e) {
      BR.mostrarErro(erro, e);
    }
  });

  document.getElementById('btn-salvar').addEventListener('click', async () => {
    if (!ultimaRequisicao) { return; }
    const erro = document.getElementById('erro-simular');
    BR.limparErro(erro);
    try {
      await BR.api.post('/cenarios', ultimaRequisicao);
      BR.toast('Cenário salvo para comparação.');
      await carregarSalvos();
    } catch (e) {
      BR.mostrarErro(erro, e);
    }
  });

  async function carregarSalvos() {
    const el = document.getElementById('lista-salvos');
    try {
      const r = await BR.api.get('/cenarios?size=20');
      el.innerHTML = r.itens.length
        ? r.itens.map((c) => `
            <tr>
              <td>${BR.esc(c.nome)}</td>
              <td>${BR.esc(BR.rotulo('tipoEmergencia', c.tipo))}</td>
              <td>${c.horizonteHoras} h</td>
              <td>${BR.num(c.percentualAfetados)}%</td>
              <td class="hint">${BR.esc(c.estrategia)}</td>
              <td>${BR.data(c.criadoEm)} · ${BR.esc(c.criadoPorNome)}</td>
              <td><button class="btn btn-secondary btn-sm" data-ver="${c.id}">Ver resultado</button></td>
            </tr>`).join('')
        : '<tr><td colspan="7">Nenhum cenário salvo ainda. Simule e salve para comparar.</td></tr>';
      el.querySelectorAll('[data-ver]').forEach((b) => b.addEventListener('click', async () => {
        try {
          const cenario = await BR.api.get('/cenarios/' + b.dataset.ver);
          mostrarResultado(cenario);
          document.getElementById('resultado').scrollIntoView({ behavior: 'smooth' });
        } catch (e) {
          BR.mostrarErro(document.getElementById('erro-salvos'), e);
        }
      }));
    } catch (e) {
      el.innerHTML = '<tr><td colspan="7">Não foi possível carregar.</td></tr>';
      BR.mostrarErro(document.getElementById('erro-salvos'), e);
    }
  }

  await carregarSalvos();
});
