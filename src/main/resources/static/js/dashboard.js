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
  } catch (e) {
    stats.innerHTML = '';
    BR.mostrarErro(document.getElementById('dash-erro'), e);
  }
});
