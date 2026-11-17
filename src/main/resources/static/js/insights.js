/* Community Insights: CRI por categoria e os quatro insights estratégicos (Etapa 4). */
BR.ready.then(async (me) => {
  if (!me || !BR.perfilStaff(me)) { return; }

  const corSituacao = { SUFICIENTE: 'var(--ok)', PARCIAL: 'var(--alert)', INSUFICIENTE: 'var(--danger)', SEM_DADOS: 'var(--gray-300)' };

  function barra(c) {
    const pct = c.percentual == null ? 0 : Math.min(Number(c.percentual), 100);
    const rotulo = c.percentual == null ? 'Sem dados' : BR.num(c.percentual) + '%';
    return `
      <div>
        <div style="display:flex;justify-content:space-between;gap:.5rem">
          <span>${BR.esc(BR.rotulo('categoria', c.categoria))}</span>
          <span>${rotulo} ${BR.badge('situacaoCobertura', c.situacao)}</span>
        </div>
        <div style="background:var(--gray-100);border-radius:999px;height:.75rem;overflow:hidden" role="img"
             aria-label="Cobertura de ${BR.esc(BR.rotulo('categoria', c.categoria))}: ${rotulo}">
          <div style="width:${pct}%;height:100%;background:${corSituacao[c.situacao] || 'var(--blue)'}"></div>
        </div>
      </div>`;
  }

  function cardCri(c) {
    return `
      <section class="panel">
        <div style="display:flex;justify-content:space-between;gap:.5rem;align-items:flex-start">
          <h3>${BR.esc(BR.rotulo('categoria', c.categoria))}</h3>
          ${BR.badge('situacaoCobertura', c.situacao)}
        </div>
        <ul class="data-list">
          <li><span class="label">Capacidade disponível</span><span>${BR.num(c.capacidadeDisponivel)} ${BR.esc(c.unidade)}</span></li>
          <li><span class="label">Demanda estimada</span><span>${BR.num(c.demandaEstimada)} ${BR.esc(c.unidade)}</span></li>
          <li><span class="label">Cobertura</span><span>${c.percentual == null ? '—' : BR.num(c.percentual) + '%'}</span></li>
          <li><span class="label">Insuficiência</span><span>${c.insuficiencia == null ? '—' : BR.num(c.insuficiencia) + ' ' + BR.esc(c.unidade)}</span></li>
          <li><span class="label">Recursos no cálculo</span><span>${c.recursosConsiderados}${c.recursosExcluidosPorUnidade ? ' (+' + c.recursosExcluidosPorUnidade + ' excluídos por unidade)' : ''}</span></li>
          <li><span class="label">Última atualização de recurso</span><span>${BR.data(c.ultimaAtualizacaoRecurso)}</span></li>
          <li><span class="label">Fonte</span><span class="hint">${BR.esc(c.fonteDados)}</span></li>
        </ul>
        ${c.observacao ? `<p class="panel-note">${BR.esc(c.observacao)}</p>` : ''}
      </section>`;
  }

  try {
    const cri = await BR.api.get('/analytics/cri');
    document.getElementById('cri-cards').innerHTML = cri.categorias.map(cardCri).join('');
    document.getElementById('grafico-cri').innerHTML = cri.categorias.map(barra).join('');
    document.getElementById('cri-fonte').textContent =
      `Calculado em ${BR.data(cri.calculadoEm)} · população base (IBGE): ${BR.num(cri.populacaoBase)} em ` +
      `${BR.num(cri.setoresConsiderados)} setor(es) · horizonte ${cri.horizonteHoras} h · método ${cri.metodo}.`;
    const lim = document.getElementById('cri-limitacoes');
    lim.hidden = false;
    lim.innerHTML = '<p><strong>Limitações:</strong></p><ul>' +
      cri.limitacoes.map((l) => `<li>${BR.esc(l)}</li>`).join('') + '</ul>';
  } catch (e) {
    BR.mostrarErro(document.getElementById('erro-cri'), e);
  }

  function tabelaGenerica(linhas, colunas, rotulos) {
    if (!linhas || !linhas.length) { return '<p class="panel-note">Sem registros.</p>'; }
    return '<div class="table-wrap"><table class="table"><thead><tr>' +
      colunas.map((c, i) => `<th>${BR.esc(rotulos[i])}</th>`).join('') +
      '</tr></thead><tbody>' +
      linhas.map((l) => '<tr>' + colunas.map((c) => `<td>${formatar(l[c])}</td>`).join('') + '</tr>').join('') +
      '</tbody></table></div>';
  }

  function formatar(v) {
    if (v == null) { return '—'; }
    if (Array.isArray(v)) { return BR.esc(v.join(', ')); }
    if (typeof v === 'object') {
      return BR.esc(Object.entries(v).map(([k, x]) => `${k}: ${x}`).join(' · '));
    }
    if (typeof v === 'number') { return BR.num(v); }
    const texto = String(v);
    return /^\d{4}-\d{2}-\d{2}T/.test(texto) ? BR.data(texto) : BR.esc(texto);
  }

  function resultadoInsight(i) {
    const r = i.resultado || {};
    switch (i.codigo) {
      case 'historical-disaster-exposure':
        return tabelaGenerica(r.porTipo,
          ['tipoDesastre', 'ocorrencias', 'pessoasAfetadas', 'desabrigados', 'desalojados', 'mortos', 'ultimaOcorrencia', 'categoriasRelacionadas'],
          ['Tipo de desastre', 'Ocorrências', 'Afetados', 'Desabrigados', 'Desalojados', 'Mortos', 'Última', 'Categorias relacionadas']);
      case 'community-resource-coverage':
        return `<p><strong>População:</strong> ${BR.num(r.populacaoTotal)} · <strong>Setores:</strong> ${BR.num(r.setores)} (com malha: ${BR.num(r.setoresComMalha)})</p>` +
          tabelaGenerica(r.setoresMaisPopulosos,
            ['codigoSetor', 'municipio', 'populacao', 'domicilios', 'recursosValidadosNaMalha'],
            ['Setor', 'Município', 'População', 'Domicílios', 'Recursos na malha']) +
          (r.setoresSemRecursos && r.setoresSemRecursos.length
            ? `<p class="panel-note"><strong>Setores populosos sem recursos:</strong> ${BR.esc(r.setoresSemRecursos.join('; '))}</p>` : '');
      case 'emergency-readiness-monitoring':
        return `<p><strong>Recursos validados sem atualização (7 dias):</strong> ${BR.num(r.recursosValidadosDesatualizados7d)} · ` +
          `<strong>Última medição importada:</strong> ${formatar(r.ultimaMedicao)}</p>` +
          tabelaGenerica(r.estacoes, ['estacao', 'municipio', 'acumulado72hMm', 'maximoMm', 'ultimaMedicao'],
            ['Estação', 'Município', 'Acumulado 72 h (mm)', 'Máximo (mm)', 'Última medição']) +
          tabelaGenerica(Object.entries(r.solicitacoesAbertasPorCategoria || {}).map(([k, v]) => ({ categoria: BR.rotulo('categoria', k), abertas: v })),
            ['categoria', 'abertas'], ['Categoria da necessidade', 'Solicitações em aberto']);
      case 'smart-resource-allocation':
        return tabelaGenerica(r.deficitsPorCategoria,
          ['categoria', 'oferta', 'demandaEstimada', 'deficit', 'unidade', 'situacao'],
          ['Categoria', 'Oferta', 'Demanda', 'Déficit', 'Unidade', 'Situação']) +
          '<h4>Recomendações (estratégia ' + BR.esc(r.estrategia) + ')</h4>' +
          tabelaGenerica(r.recomendacoes, ['prioridade', 'categoria', 'acao', 'justificativa'],
            ['#', 'Categoria', 'Ação recomendada', 'Justificativa']) +
          (r.areasPrioritarias && r.areasPrioritarias.length
            ? `<p class="panel-note"><strong>Áreas prioritárias:</strong> ${BR.esc(r.areasPrioritarias.join('; '))}</p>` : '');
      default:
        return '<pre>' + BR.esc(JSON.stringify(r, null, 2)) + '</pre>';
    }
  }

  try {
    const insights = await BR.api.get('/analytics/insights');
    document.getElementById('insights').innerHTML = insights.map((i) => `
      <section class="panel">
        <h3>${BR.esc(i.titulo)}</h3>
        <p><strong>Pergunta estratégica:</strong> ${BR.esc(i.perguntaEstrategica)}</p>
        ${i.dadosSuficientes ? '' : '<div class="alert-erro" role="note">Não existem dados suficientes para gerar esta análise por completo. Importe as fontes na tela Dados externos.</div>'}
        ${resultadoInsight(i)}
        <p><strong>Interpretação:</strong> ${BR.esc(i.interpretacao)}</p>
        <p><strong>Decisão recomendada:</strong> ${BR.esc(i.decisaoRecomendada)}</p>
        <p><strong>Impacto potencial:</strong> ${BR.esc(i.impactoPotencial)}</p>
        <details>
          <summary>Explicabilidade: dados, fontes, metodologia e limitações</summary>
          <ul class="data-list">
            <li><span class="label">Dados utilizados</span><span>${BR.esc(i.dadosUtilizados.join('; '))}</span></li>
            <li><span class="label">Fontes</span><span>${BR.esc(i.fontes.join('; '))}</span></li>
            <li><span class="label">Metodologia</span><span>${BR.esc(i.metodologia)}</span></li>
            <li><span class="label">Limitações</span><span>${BR.esc(i.limitacoes.join(' '))}</span></li>
            <li><span class="label">Gerado em</span><span>${BR.data(i.geradoEm)}</span></li>
          </ul>
        </details>
      </section>`).join('');
  } catch (e) {
    BR.mostrarErro(document.getElementById('erro-insights'), e);
  }
});
