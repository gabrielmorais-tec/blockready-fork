/* Dados externos: fontes, importação com pré-visualização e histórico (Etapa 3). */
BR.ready.then(async (me) => {
  if (!me || !BR.perfilStaff(me)) { return; }

  const selFonte = document.getElementById('i-fonte');
  const abrangencia = { SETOR_CENSITARIO: 'Setor censitário', ESTACAO: 'Ponto da estação', MUNICIPIO: 'Município' };
  let fontes = [];
  let pagina = 0;

  async function carregarFontes() {
    const el = document.getElementById('lista-fontes');
    try {
      fontes = await BR.api.get('/fontes');
      el.innerHTML = fontes.map((f) => `
        <tr>
          <td><strong>${BR.esc(f.nome)}</strong><br><span class="hint">${BR.esc(f.descricao || '')}</span><br>
              ${f.urlBase ? `<a href="${BR.esc(f.urlBase)}" target="_blank" rel="noopener">Documentação oficial</a>` : ''}</td>
          <td><span class="badge badge-info">${BR.esc(abrangencia[f.abrangencia] || f.abrangencia)}</span></td>
          <td>${BR.num(f.totalRegistros)}</td>
          <td>${BR.data(f.ultimaImportacao)}</td>
          <td><span class="hint">${BR.esc(f.colunasEsperadas || '—')}</span></td>
        </tr>`).join('');
      selFonte.innerHTML = fontes.map((f) => `<option value="${BR.esc(f.codigo)}">${BR.esc(f.nome)}</option>`).join('');
    } catch (e) {
      el.innerHTML = '<tr><td colspan="5">Não foi possível carregar.</td></tr>';
      BR.mostrarErro(document.getElementById('erro-fontes'), e);
    }
  }

  function formData() {
    const arquivo = document.getElementById('i-arquivo').files[0];
    if (!arquivo) { return null; }
    const fd = new FormData();
    fd.append('fonte', selFonte.value);
    fd.append('arquivo', arquivo);
    return fd;
  }

  document.getElementById('btn-previa').addEventListener('click', async () => {
    const erro = document.getElementById('erro-importar');
    BR.limparErro(erro);
    const fd = formData();
    if (!fd) { BR.toast('Selecione um arquivo CSV.', true); return; }
    try {
      const p = await BR.api.postForm('/importacoes/previa', fd);
      document.getElementById('previa').hidden = false;
      document.getElementById('previa-info').textContent =
        `${p.totalLinhas} linha(s) de dados, delimitador "${p.delimitador}". Mostrando as primeiras ${p.linhas.length}.`;
      document.getElementById('previa-cab').innerHTML =
        '<tr>' + p.colunas.map((c) => `<th>${BR.esc(c)}</th>`).join('') + '</tr>';
      document.getElementById('previa-linhas').innerHTML = p.linhas.map((linha) =>
        '<tr>' + p.colunas.map((c) => `<td>${BR.esc(linha[c])}</td>`).join('') + '</tr>').join('');
    } catch (e) {
      document.getElementById('previa').hidden = true;
      BR.mostrarErro(erro, e);
    }
  });

  document.getElementById('form-importar').addEventListener('submit', async (ev) => {
    ev.preventDefault();
    const erro = document.getElementById('erro-importar');
    BR.limparErro(erro);
    const fd = formData();
    if (!fd) { BR.toast('Selecione um arquivo CSV.', true); return; }
    fd.append('periodoReferencia', document.getElementById('i-periodo').value);
    fd.append('demonstrativo', document.getElementById('i-demo').value);
    const btn = document.getElementById('btn-importar');
    btn.disabled = true;
    btn.textContent = 'Importando…';
    try {
      const r = await BR.api.postForm('/importacoes', fd);
      BR.toast(`Importação concluída: ${r.totalProcessados} processado(s), ${r.totalRejeitados} rejeitado(s).`,
        r.status === 'FALHA');
      document.getElementById('previa').hidden = true;
      document.getElementById('form-importar').reset();
      pagina = 0;
      await Promise.all([carregarFontes(), carregarHistorico()]);
    } catch (e) {
      BR.mostrarErro(erro, e);
    } finally {
      btn.disabled = false;
      btn.textContent = 'Importar';
    }
  });

  async function carregarHistorico() {
    const el = document.getElementById('lista-historico');
    try {
      const r = await BR.api.get(`/importacoes?page=${pagina}&size=10`);
      if (!r.itens.length) {
        el.innerHTML = '<tr><td colspan="9">Nenhuma importação realizada ainda.</td></tr>';
      } else {
        el.innerHTML = r.itens.map((i) => `
          <tr>
            <td>${BR.data(i.criadoEm)}</td>
            <td>${BR.esc(i.fonteCodigo)}</td>
            <td>${BR.esc(i.nomeArquivo)}${i.demonstrativo ? ' <span class="badge badge-alert">Demonstrativo</span>' : ''}</td>
            <td>${BR.esc(i.periodoReferencia || '—')}</td>
            <td>${BR.num(i.totalProcessados)}</td>
            <td>${BR.num(i.totalRejeitados)}</td>
            <td>${BR.badge('statusImportacao', i.status)}</td>
            <td>${BR.esc(i.importadoPorNome)}</td>
            <td>${i.erros ? `<button class="btn btn-secondary btn-sm" data-erros="${i.id}">Erros</button>` : ''}</td>
          </tr>`).join('');
        el.querySelectorAll('[data-erros]').forEach((b) => b.addEventListener('click', () => {
          const item = r.itens.find((x) => String(x.id) === b.dataset.erros);
          document.getElementById('erros-texto').textContent = item.erros || '';
          document.getElementById('dlg-erros').showModal();
        }));
      }
      const pager = document.getElementById('pager-historico');
      pager.innerHTML = r.totalPaginas > 1
        ? `<button class="btn btn-secondary btn-sm" id="pg-ant" ${pagina === 0 ? 'disabled' : ''}>Anterior</button>
           <span>Página ${r.pagina + 1} de ${r.totalPaginas}</span>
           <button class="btn btn-secondary btn-sm" id="pg-prox" ${pagina + 1 >= r.totalPaginas ? 'disabled' : ''}>Próxima</button>`
        : '';
      const ant = document.getElementById('pg-ant');
      const prox = document.getElementById('pg-prox');
      if (ant) { ant.addEventListener('click', () => { pagina--; carregarHistorico(); }); }
      if (prox) { prox.addEventListener('click', () => { pagina++; carregarHistorico(); }); }
    } catch (e) {
      el.innerHTML = '<tr><td colspan="9">Não foi possível carregar.</td></tr>';
      BR.mostrarErro(document.getElementById('erro-historico'), e);
    }
  }

  document.querySelectorAll('dialog [data-fechar]').forEach((b) =>
    b.addEventListener('click', () => b.closest('dialog').close()));

  await Promise.all([carregarFontes(), carregarHistorico()]);
});
