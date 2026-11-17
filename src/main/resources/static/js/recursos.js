/* Gestão de recursos: consulta com filtros, disponibilidade, validação, histórico e desativação. */
BR.ready.then((me) => {
  if (!me) { return; }
  const staff = BR.perfilStaff(me);
  const f = document.getElementById('filtros');
  const corpo = document.getElementById('lista');
  const erro = document.getElementById('erro');
  const pager = document.getElementById('pager');
  let pagina = 0;
  let itens = [];

  f.categoria.innerHTML = BR.options('categoria', 'Todas');
  f.status.innerHTML = BR.options('statusRecurso', 'Todos');
  if (!staff && me.perfil !== 'ORGANIZACAO') { f.status.closest('.field').hidden = true; }
  if (['ORGANIZACAO', 'GESTOR', 'ADMIN'].includes(me.perfil)) { document.getElementById('novo').hidden = false; }

  const podeEscrever = (r) => staff || (me.perfil === 'ORGANIZACAO' && r.organizacaoId === me.organizacaoId);

  function linha(r) {
    const esc = BR.esc;
    const acoes = [];
    if (podeEscrever(r) && r.status !== 'INATIVO') {
      acoes.push(`<button class="btn btn-secondary btn-sm" data-act="disp" data-id="${r.id}">Disponibilidade</button>`);
      acoes.push(`<a class="btn btn-secondary btn-sm" href="/pages/recurso-form.html?id=${r.id}">Editar</a>`);
    }
    if (podeEscrever(r)) { acoes.push(`<button class="btn btn-secondary btn-sm" data-act="hist" data-id="${r.id}">Histórico</button>`); }
    if (staff && r.status === 'PENDENTE_VALIDACAO') {
      acoes.push(`<button class="btn btn-primary btn-sm" data-act="validar" data-id="${r.id}">Validar</button>`);
      acoes.push(`<button class="btn btn-alert btn-sm" data-act="rejeitar" data-id="${r.id}">Rejeitar</button>`);
    }
    if (podeEscrever(r) && r.status !== 'INATIVO') {
      acoes.push(`<button class="btn btn-secondary btn-sm" data-act="desativar" data-id="${r.id}">Desativar</button>`);
    }
    const detalhes = Object.entries(r.detalhes || {}).filter(([, v]) => v !== null && v !== false && v !== '')
      .map(([k, v]) => (v === true ? k : `${k}: ${v}`)).join(' · ');
    return `<tr>
      <td><strong>${esc(r.nome)}</strong><span class="sub">${esc(r.endereco)}</span>${detalhes ? `<span class="sub">${esc(detalhes)}</span>` : ''}</td>
      <td>${esc(BR.rotulo('categoria', r.categoria))}</td>
      <td>${BR.badge('statusRecurso', r.status)}${r.confirmado ? '' : '<span class="sub">Não confirmado</span>'}</td>
      <td>${esc(r.descricaoCapacidade)}${r.acessivel ? '<span class="sub">Acessível</span>' : ''}</td>
      <td>${esc(r.organizacaoNome)}<span class="sub">${esc(r.horarioFuncionamento || '')}</span></td>
      <td>${BR.data(r.atualizadoEm)}</td>
      <td><div class="actions">${acoes.join('')}</div></td></tr>`;
  }

  async function carregar() {
    BR.limparErro(erro);
    const q = new URLSearchParams({ page: pagina, size: 15 });
    ['categoria', 'status', 'busca'].forEach((k) => { if (f[k].value.trim()) { q.set(k, f[k].value.trim()); } });
    if (f.lat.value && f.lon.value && f.raio.value) { q.set('lat', f.lat.value); q.set('lon', f.lon.value); q.set('raioKm', f.raio.value); }
    try {
      const p = await BR.api.get('/recursos?' + q);
      itens = p.itens;
      corpo.innerHTML = itens.length ? itens.map(linha).join('')
        : '<tr><td colspan="7">Nenhum recurso encontrado para estes filtros.</td></tr>';
      pager.innerHTML = `<span>${BR.num(p.total)} recurso(s) · página ${p.pagina + 1} de ${Math.max(p.totalPaginas, 1)}</span>
        <button class="btn btn-secondary btn-sm" id="ant" ${p.pagina === 0 ? 'disabled' : ''}>Anterior</button>
        <button class="btn btn-secondary btn-sm" id="prox" ${p.pagina + 1 >= p.totalPaginas ? 'disabled' : ''}>Próxima</button>`;
      document.getElementById('ant').onclick = () => { pagina--; carregar(); };
      document.getElementById('prox').onclick = () => { pagina++; carregar(); };
    } catch (e) { BR.mostrarErro(erro, e); }
  }

  f.addEventListener('submit', (e) => { e.preventDefault(); pagina = 0; carregar(); });
  document.getElementById('limpar').addEventListener('click', () => { f.reset(); pagina = 0; carregar(); });
  document.getElementById('geo').addEventListener('click', () => {
    if (!navigator.geolocation) { BR.toast('Geolocalização indisponível neste navegador.', true); return; }
    navigator.geolocation.getCurrentPosition((pos) => {
      f.lat.value = pos.coords.latitude.toFixed(5);
      f.lon.value = pos.coords.longitude.toFixed(5);
      if (!f.raio.value) { f.raio.value = 5; }
    }, () => BR.toast('Não foi possível obter sua localização.', true));
  });

  // Diálogos
  const dlgDisp = document.getElementById('dlg-disp');
  const formDisp = document.getElementById('form-disp');
  const dlgHist = document.getElementById('dlg-hist');
  let alvo = null;

  async function abrirHistorico(id) {
    const [hist, disp] = await Promise.all([BR.api.get(`/recursos/${id}/historico`), BR.api.get(`/recursos/${id}/disponibilidades?size=10`)]);
    document.getElementById('hist-lista').innerHTML = hist.length
      ? hist.map((h) => `<li><strong>${BR.esc(h.acao)}</strong> — ${BR.esc(h.descricao || '')}<span class="when">${BR.data(h.criadoEm)} · ${BR.esc(h.usuarioNome)}</span></li>`).join('')
      : '<li>Sem registros.</li>';
    document.getElementById('disp-lista').innerHTML = disp.itens.length
      ? disp.itens.map((d) => `<li>${BR.num(d.capacidadeDisponivel)} de ${BR.num(d.capacidadeTotal)}${d.observacao ? ' — ' + BR.esc(d.observacao) : ''}<span class="when">${BR.data(d.registradoEm)} · ${BR.esc(d.registradoPorNome)}</span></li>`).join('')
      : '<li>Sem registros.</li>';
    dlgHist.showModal();
  }

  corpo.addEventListener('click', async (e) => {
    const b = e.target.closest('[data-act]');
    if (!b) { return; }
    const id = b.dataset.id;
    const r = itens.find((x) => String(x.id) === id);
    try {
      if (b.dataset.act === 'disp') {
        alvo = r;
        formDisp.total.value = r.capacidadeTotal;
        formDisp.disponivel.value = r.capacidadeDisponivel;
        formDisp.observacao.value = '';
        BR.limparErro(document.getElementById('erro-disp'));
        dlgDisp.showModal();
      } else if (b.dataset.act === 'hist') {
        await abrirHistorico(id);
      } else if (b.dataset.act === 'validar') {
        await BR.api.post(`/recursos/${id}/validar`);
        BR.toast('Recurso validado.'); carregar();
      } else if (b.dataset.act === 'rejeitar') {
        const motivo = window.prompt('Motivo da rejeição (opcional):');
        if (motivo === null) { return; }
        await BR.api.post(`/recursos/${id}/rejeitar`, { motivo: motivo || null });
        BR.toast('Recurso rejeitado.'); carregar();
      } else if (b.dataset.act === 'desativar') {
        if (!window.confirm('Desativar este recurso? O histórico será preservado.')) { return; }
        await BR.api.del(`/recursos/${id}`);
        BR.toast('Recurso desativado.'); carregar();
      }
    } catch (err) { BR.toast(err.message, true); }
  });

  formDisp.addEventListener('submit', async (e) => {
    e.preventDefault();
    try {
      await BR.api.patch(`/recursos/${alvo.id}/disponibilidade`, {
        capacidadeTotal: Number(formDisp.total.value),
        capacidadeDisponivel: Number(formDisp.disponivel.value),
        observacao: formDisp.observacao.value.trim() || null
      });
      dlgDisp.close();
      BR.toast('Disponibilidade atualizada.');
      carregar();
    } catch (err) { BR.mostrarErro(document.getElementById('erro-disp'), err); }
  });
  document.querySelectorAll('[data-fechar]').forEach((b) => b.addEventListener('click', () => b.closest('dialog').close()));

  carregar();
});
