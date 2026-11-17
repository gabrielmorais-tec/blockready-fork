/* Resource Map: recursos, malha dos setores (IBGE) e estações (CEMADEN) com Leaflet + PostGIS (Etapa 5). */
BR.ready.then(async (me) => {
  if (!me) { return; }

  const CORES = { AGUA: '#1f6fe5', ABRIGO: '#8a4fd8', ENERGIA: '#ff8a1f', ALIMENTACAO: '#138a5b', COMUNICACAO: '#19c2e3', SAUDE: '#c2331f' };
  const staff = BR.perfilStaff(me);
  const nota = document.getElementById('mapa-nota');
  const erroEl = document.getElementById('erro-mapa');

  document.getElementById('m-cat').innerHTML = BR.options('categoria', 'Todas as categorias');
  if (staff) {
    document.getElementById('campo-status').hidden = false;
    document.getElementById('m-status').innerHTML = BR.options('statusRecurso', 'Todos os status');
  }

  if (typeof L === 'undefined') {
    nota.textContent = 'Não foi possível carregar a biblioteca do mapa (Leaflet). Verifique a conexão com a internet.';
    return;
  }

  const mapa = L.map('mapa').setView([-15.78, -47.93], 4);
  L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 19,
    attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
  }).addTo(mapa);

  const camadaRecursos = L.layerGroup().addTo(mapa);
  const camadaSetores = L.layerGroup().addTo(mapa);
  const camadaEstacoes = L.layerGroup().addTo(mapa);

  async function carregar() {
    BR.limparErro(erroEl);
    camadaRecursos.clearLayers();
    camadaSetores.clearLayers();
    camadaEstacoes.clearLayers();
    const pontos = [];

    // Recursos: moradores veem apenas validados (regra do backend); staff pode filtrar status.
    let totalRecursos = 0;
    try {
      const categoria = document.getElementById('m-cat').value;
      const status = staff ? document.getElementById('m-status').value : '';
      let caminho = '/recursos?size=100' + (categoria ? '&categoria=' + categoria : '') + (status ? '&status=' + status : '');
      const r = await BR.api.get(caminho);
      totalRecursos = r.total;
      r.itens.forEach((rec) => {
        const cor = CORES[rec.categoria] || '#1f6fe5';
        const marcador = L.circleMarker([rec.latitude, rec.longitude], {
          radius: 8, color: '#ffffff', weight: 2, fillColor: cor, fillOpacity: rec.confirmado ? 0.95 : 0.45
        });
        marcador.bindPopup(`
          <strong>${BR.esc(rec.nome)}</strong><br>
          ${BR.esc(BR.rotulo('categoria', rec.categoria))} · ${BR.badge('statusRecurso', rec.status)}<br>
          ${BR.esc(rec.descricaoCapacidade || '')}<br>
          <span class="hint">${BR.esc(rec.endereco)}</span><br>
          ${rec.horarioFuncionamento ? 'Horário: ' + BR.esc(rec.horarioFuncionamento) + '<br>' : ''}
          ${rec.acessivel ? 'Acessível ♿<br>' : ''}
          Atualizado: ${BR.data(rec.atualizadoEm)}<br>
          <a href="/pages/recursos.html">Ver na lista de recursos</a>`);
        marcador.addTo(camadaRecursos);
        pontos.push([rec.latitude, rec.longitude]);
      });
    } catch (e) {
      BR.mostrarErro(erroEl, e);
    }

    // Malha dos setores censitários (quando importada).
    if (document.getElementById('m-setores').checked) {
      try {
        const geo = await BR.api.get('/territorios/setores/geojson');
        if (geo.features.length) {
          const camada = L.geoJSON(geo, {
            style: { color: '#1b3a6b', weight: 1, fillColor: '#19c2e3', fillOpacity: 0.08 },
            onEachFeature: (f, layer) => layer.bindPopup(
              `<strong>Setor ${BR.esc(f.properties.codigoSetor)}</strong><br>` +
              `${BR.esc(f.properties.municipio)}/${BR.esc(f.properties.uf)}<br>` +
              `População: ${BR.num(f.properties.populacao)} · Domicílios: ${BR.num(f.properties.domicilios)}` +
              (f.properties.demonstrativo ? '<br><em>Dados demonstrativos</em>' : ''))
          });
          camada.addTo(camadaSetores);
          const b = camada.getBounds();
          if (b.isValid()) { pontos.push([b.getSouth(), b.getWest()], [b.getNorth(), b.getEast()]); }
        }
      } catch (e) { /* malha é opcional no mapa */ }
    }

    // Estações pluviométricas (últimas 72 h).
    if (document.getElementById('m-estacoes').checked) {
      try {
        const estacoes = await BR.api.get('/territorios/estacoes?horas=72');
        estacoes.forEach((e) => {
          const marcador = L.circleMarker([e.latitude, e.longitude], {
            radius: 6, color: '#ffffff', weight: 2, fillColor: '#0a1a33', fillOpacity: 0.95
          });
          marcador.bindPopup(`
            <strong>Estação ${BR.esc(e.codigo)}</strong>${e.nome ? ' · ' + BR.esc(e.nome) : ''}<br>
            ${BR.esc(e.municipio)}/${BR.esc(e.uf)}<br>
            Acumulado 72 h: ${BR.num(e.acumuladoMm)} mm (máx. ${BR.num(e.maximoMm)} mm)<br>
            Última medição: ${BR.data(e.ultimaMedicao)}<br>
            <span class="hint">Medições valem para o ponto da estação (CEMADEN, UTC).</span>`);
          marcador.addTo(camadaEstacoes);
          pontos.push([e.latitude, e.longitude]);
        });
      } catch (e) { /* estações são opcionais no mapa */ }
    }

    if (pontos.length) {
      mapa.fitBounds(L.latLngBounds(pontos).pad(0.2));
    }
    nota.textContent = totalRecursos
      ? `${BR.num(totalRecursos)} recurso(s) no filtro atual (mostrando até 100 no mapa). ` +
        'Recursos pendentes aparecem translúcidos e não contam como confirmados.'
      : 'Nenhum recurso para exibir com os filtros atuais. Cadastre recursos ou ajuste os filtros.';
  }

  document.getElementById('filtros').addEventListener('submit', (ev) => { ev.preventDefault(); carregar(); });
  document.getElementById('m-setores').addEventListener('change', carregar);
  document.getElementById('m-estacoes').addEventListener('change', carregar);
  await carregar();
});
