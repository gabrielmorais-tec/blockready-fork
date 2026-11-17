/* Territórios: resumo da base importada, setores censitários e estações (Etapa 3). */
BR.ready.then(async (me) => {
  if (!me || !BR.perfilStaff(me)) { return; }
  let pagina = 0;

  try {
    const r = await BR.api.get('/territorios/resumo');
    const card = (valor, rotulo) => `<div class="stat"><strong>${BR.num(valor)}</strong><span>${rotulo}</span></div>`;
    document.getElementById('resumo').innerHTML =
      card(r.setores, 'Setores censitários importados') +
      card(r.populacaoTotal, 'População registrada (IBGE)') +
      card(r.domiciliosTotal, 'Domicílios registrados') +
      card(r.setoresComGeometria, 'Setores com malha geográfica') +
      card(r.estacoesMeteorologicas, 'Estações pluviométricas (CEMADEN)') +
      card(r.ocorrenciasDesastre, 'Ocorrências históricas (Atlas)');
  } catch (e) {
    BR.mostrarErro(document.getElementById('erro-resumo'), e);
  }

  async function carregarSetores() {
    const el = document.getElementById('lista-setores');
    try {
      const r = await BR.api.get(`/territorios/setores?page=${pagina}&size=15`);
      if (!r.itens.length) {
        el.innerHTML = '<tr><td colspan="7">Nenhum setor importado. Use a tela <a href="/pages/dados.html">Dados externos</a> para importar os agregados do Censo 2022.</td></tr>';
      } else {
        el.innerHTML = r.itens.map((s) => `
          <tr>
            <td>${BR.esc(s.codigoSetor)}</td>
            <td>${BR.esc(s.municipio)}/${BR.esc(s.uf)}</td>
            <td>${BR.num(s.populacao)}</td>
            <td>${BR.num(s.domicilios)}</td>
            <td>${s.areaKm2 == null ? '—' : BR.num(s.areaKm2)}</td>
            <td>${s.possuiGeometria ? '<span class="badge badge-ok">Importada</span>' : '<span class="badge badge-neutral">Sem malha</span>'}</td>
            <td>${s.demonstrativo ? '<span class="badge badge-alert">Demonstrativo</span>' : '<span class="badge badge-info">Oficial importado</span>'}</td>
          </tr>`).join('');
      }
      const pager = document.getElementById('pager-setores');
      pager.innerHTML = r.totalPaginas > 1
        ? `<button class="btn btn-secondary btn-sm" id="st-ant" ${pagina === 0 ? 'disabled' : ''}>Anterior</button>
           <span>Página ${r.pagina + 1} de ${r.totalPaginas}</span>
           <button class="btn btn-secondary btn-sm" id="st-prox" ${pagina + 1 >= r.totalPaginas ? 'disabled' : ''}>Próxima</button>`
        : '';
      const ant = document.getElementById('st-ant');
      const prox = document.getElementById('st-prox');
      if (ant) { ant.addEventListener('click', () => { pagina--; carregarSetores(); }); }
      if (prox) { prox.addEventListener('click', () => { pagina++; carregarSetores(); }); }
    } catch (e) {
      el.innerHTML = '<tr><td colspan="7">Não foi possível carregar.</td></tr>';
      BR.mostrarErro(document.getElementById('erro-setores'), e);
    }
  }

  async function carregarEstacoes() {
    const el = document.getElementById('lista-estacoes');
    try {
      const estacoes = await BR.api.get('/territorios/estacoes?horas=72');
      el.innerHTML = estacoes.length
        ? estacoes.map((e) => `
            <tr>
              <td>${BR.esc(e.codigo)}${e.nome ? ' · ' + BR.esc(e.nome) : ''}</td>
              <td>${BR.esc(e.municipio)}/${BR.esc(e.uf)}</td>
              <td>${BR.num(e.acumuladoMm)}</td>
              <td>${BR.num(e.maximoMm)}</td>
              <td>${BR.data(e.ultimaMedicao)}</td>
            </tr>`).join('')
        : '<tr><td colspan="5">Sem medições nas últimas 72 h. Importe dados do CEMADEN em <a href="/pages/dados.html">Dados externos</a>.</td></tr>';
    } catch (e) {
      el.innerHTML = '<tr><td colspan="5">Não foi possível carregar.</td></tr>';
      BR.mostrarErro(document.getElementById('erro-estacoes'), e);
    }
  }

  await Promise.all([carregarSetores(), carregarEstacoes()]);
});
