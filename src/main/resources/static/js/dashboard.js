/* Painel: estado do sistema (health) e indicadores lidos do banco (/api/v1/dashboard). */
BR.ready.then(async (me) => {
  if (!me) { return; }
  const status = document.getElementById('system-status');
  const linha = (rotulo, dica, ok, texto) =>
    `<li><span class="label">${BR.esc(rotulo)}<span class="hint">${BR.esc(dica)}</span></span><span class="badge ${ok ? 'badge-ok' : 'badge-danger'}">${texto}</span></li>`;

  try {
    const { body } = await BR.api.raw('/health');
    if (!body || !body.database) { throw new Error('resposta inesperada'); }
    status.innerHTML =
      linha('Aplicação', 'Versão ' + body.version, body.status === 'UP', body.status === 'UP' ? 'Operando' : 'Com falhas') +
      linha('Banco de dados', 'PostgreSQL', body.database.status === 'UP', body.database.status === 'UP' ? 'Conectado' : 'Indisponível') +
      linha('PostGIS', body.postgis.detail || '', body.postgis.status === 'UP', body.postgis.status === 'UP' ? 'Ativo' : 'Indisponível');
  } catch (_) {
    status.innerHTML = '<li><span class="label">Não foi possível consultar /api/v1/health</span><span class="badge badge-alert">Sem resposta</span></li>';
  }

  const stats = document.getElementById('stats');
  const detalhes = document.getElementById('dash-detalhes');
  try {
    const d = await BR.api.get('/dashboard');
    const card = (valor, rotulo, alerta) => `<div class="stat${alerta ? ' alerta' : ''}"><strong>${BR.num(valor)}</strong><span>${rotulo}</span></div>`;
    stats.innerHTML =
      card(d.recursosValidados, 'Recursos confirmados') +
      card(d.recursosPendentesValidacao, 'Recursos pendentes de validação', d.recursosPendentesValidacao > 0) +
      card(d.solicitacoesEmAberto, 'Solicitações em aberto', d.solicitacoesEmAberto > 0) +
      card(d.solicitacoesAtendidas, 'Solicitações atendidas') +
      card(d.voluntariosAtivos, 'Voluntários ativos') +
      card(d.organizacoesPendentes, 'Organizações aguardando aprovação', d.organizacoesPendentes > 0);

    const lista = (mapa, grupo) => Object.entries(mapa).map(([k, v]) =>
      `<li><span class="label">${BR.esc(BR.rotulo(grupo, k))}</span><span class="badge ${v > 0 ? 'badge-info' : 'badge-neutral'}">${v > 0 ? BR.num(v) : 'Sem registros'}</span></li>`).join('');
    detalhes.innerHTML =
      `<section class="panel"><h2>Recursos confirmados por categoria</h2><ul class="data-list">${lista(d.recursosValidadosPorCategoria, 'categoria')}</ul></section>` +
      `<section class="panel"><h2>Solicitações por status</h2><ul class="data-list">${lista(d.solicitacoesPorStatus, 'statusSolicitacao')}</ul></section>`;
    document.getElementById('dash-fonte').textContent = 'Contagens lidas do banco de dados em ' + BR.data(d.geradoEm) + '.';

    // Alertas informativos derivados dos registros do banco.
    const alertas = [];
    if (d.recursosPendentesValidacao > 0) { alertas.push([d.recursosPendentesValidacao + ' recurso(s) aguardando validação', 'alert']); }
    if (d.solicitacoesEmAberto > 0) { alertas.push([d.solicitacoesEmAberto + ' solicitação(ões) em aberto', 'alert']); }
    if (d.organizacoesPendentes > 0) { alertas.push([d.organizacoesPendentes + ' organização(ões) aguardando aprovação', 'info']); }
    window.__alertasBase = alertas;
  } catch (e) {
    stats.innerHTML = '';
    BR.mostrarErro(document.getElementById('dash-erro'), e);
  }

  // Community Resilience Index agregada (dados do backend; sem números estáticos).
  const corSituacao = { SUFICIENTE: 'var(--ok)', PARCIAL: 'var(--alert)', INSUFICIENTE: 'var(--danger)', SEM_DADOS: 'var(--gray-300)' };
  const alertasEl = document.getElementById('alertas');
  const alertas = window.__alertasBase || [];
  try {
    const cri = await BR.api.get('/analytics/cri');
    document.getElementById('cri-resumo').innerHTML = cri.categorias.map((c) => {
      const pct = c.percentual == null ? 0 : Math.min(Number(c.percentual), 100);
      const rotulo = c.percentual == null ? 'Sem dados' : BR.num(c.percentual) + '%';
      return `<div>
        <div style="display:flex;justify-content:space-between;gap:.5rem">
          <span>${BR.esc(BR.rotulo('categoria', c.categoria))}</span>
          <span>${rotulo} ${BR.badge('situacaoCobertura', c.situacao)}</span>
        </div>
        <div style="background:var(--gray-100);border-radius:999px;height:.6rem;overflow:hidden" role="img"
             aria-label="Cobertura de ${BR.esc(BR.rotulo('categoria', c.categoria))}: ${rotulo}">
          <div style="width:${pct}%;height:100%;background:${corSituacao[c.situacao] || 'var(--blue)'}"></div>
        </div></div>`;
    }).join('');
    document.getElementById('cri-nota').textContent = cri.dadosSuficientes
      ? `Cobertura estimada para ${BR.num(cri.populacaoBase)} pessoas (IBGE) em ${cri.horizonteHoras} h. ` +
        'Cobertura de 100% não garante a segurança da comunidade.'
      : 'Sem dados do IBGE importados: o CRI aparece como SEM_DADOS. Gestores podem importar na tela Dados externos.';
    cri.categorias.filter((c) => c.situacao === 'INSUFICIENTE').forEach((c) =>
      alertas.push(['Cobertura insuficiente de ' + BR.rotulo('categoria', c.categoria) +
        (c.insuficiencia != null ? ' (déficit de ' + BR.num(c.insuficiencia) + ' ' + c.unidade + ')' : ''), 'danger']));
    if (!cri.dadosSuficientes) { alertas.push(['CRI sem base populacional: importe os dados do IBGE', 'info']); }
  } catch (e) {
    document.getElementById('cri-nota').textContent = 'Não foi possível calcular o CRI agora.';
  }
  alertasEl.innerHTML = alertas.length
    ? alertas.map(([texto, classe]) => `<li><span class="label">${BR.esc(texto)}</span><span class="badge badge-${classe}">Atenção</span></li>`).join('')
    : '<li><span class="label">Nenhum alerta no momento</span><span class="badge badge-ok">Tudo em dia</span></li>';
});
