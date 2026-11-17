/* Necessidades da comunidade: morador registra; a lista mostra as solicitações geradas e seu andamento. */
BR.ready.then((me) => {
  if (!me) { return; }
  const form = document.getElementById('form-necessidade');
  const erro = document.getElementById('erro');
  const corpo = document.getElementById('lista');
  const dlg = document.getElementById('dlg-hist');
  let pagina = 0;
  let itens = [];

  form.categoria.innerHTML = BR.options('categoria');
  form.prioridade.innerHTML = BR.options('prioridade');
  form.prioridade.value = 'MEDIA';

  async function carregar() {
    try {
      const p = await BR.api.get(`/solicitacoes?page=${pagina}&size=10`);
      itens = p.itens;
      corpo.innerHTML = itens.length ? itens.map((s) => `<tr>
        <td>${BR.data(s.criadoEm)}</td>
        <td>${BR.esc(BR.rotulo('categoria', s.categoria))}</td>
        <td>${BR.esc(s.descricao)}<span class="sub">${BR.num(s.quantidade)} ${BR.esc(s.unidade)}</span></td>
        <td>${BR.badge('prioridade', s.prioridade)}</td>
        <td>${BR.badge('statusSolicitacao', s.status)}</td>
        <td><div class="actions">
          <button class="btn btn-secondary btn-sm" data-act="hist" data-id="${s.id}">Acompanhar</button>
          ${s.proximosStatus.includes('CANCELADA') ? `<button class="btn btn-secondary btn-sm" data-act="cancelar" data-id="${s.id}">Cancelar</button>` : ''}
        </div></td></tr>`).join('')
        : '<tr><td colspan="6">Você ainda não registrou necessidades.</td></tr>';
      document.getElementById('pager').innerHTML = `<span>${BR.num(p.total)} registro(s)</span>
        <button class="btn btn-secondary btn-sm" id="ant" ${p.pagina === 0 ? 'disabled' : ''}>Anterior</button>
        <button class="btn btn-secondary btn-sm" id="prox" ${p.pagina + 1 >= p.totalPaginas ? 'disabled' : ''}>Próxima</button>`;
      document.getElementById('ant').onclick = () => { pagina--; carregar(); };
      document.getElementById('prox').onclick = () => { pagina++; carregar(); };
    } catch (e) { BR.mostrarErro(erro, e); }
  }

  if (me.perfil !== 'MORADOR') {
    form.closest('section').hidden = true;
    document.getElementById('so-morador').hidden = false;
  }

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    BR.limparErro(erro);
    const lat = form.latitude.value, lon = form.longitude.value;
    try {
      await BR.api.post('/necessidades', {
        categoria: form.categoria.value, descricao: form.descricao.value.trim(),
        quantidade: Number(form.quantidade.value), unidade: form.unidade.value.trim(),
        endereco: form.endereco.value.trim(), latitude: lat ? Number(lat) : null, longitude: lon ? Number(lon) : null,
        prioridade: form.prioridade.value
      });
      form.reset();
      form.prioridade.value = 'MEDIA';
      BR.toast('Necessidade registrada. Acompanhe a solicitação abaixo.');
      pagina = 0;
      carregar();
    } catch (err) { BR.mostrarErro(erro, err); }
  });

  corpo.addEventListener('click', async (e) => {
    const b = e.target.closest('[data-act]');
    if (!b) { return; }
    try {
      if (b.dataset.act === 'hist') {
        const h = await BR.api.get(`/solicitacoes/${b.dataset.id}/historico`);
        document.getElementById('hist-lista').innerHTML = h.map((x) =>
          `<li><strong>${BR.esc(BR.rotulo('statusSolicitacao', x.statusNovo))}</strong>${x.observacao ? ' — ' + BR.esc(x.observacao) : ''}<span class="when">${BR.data(x.criadoEm)}${x.usuarioNome ? ' · ' + BR.esc(x.usuarioNome) : ''}</span></li>`).join('');
        dlg.showModal();
      } else if (b.dataset.act === 'cancelar') {
        if (!window.confirm('Cancelar esta solicitação?')) { return; }
        await BR.api.patch(`/solicitacoes/${b.dataset.id}/status`, { status: 'CANCELADA', observacao: 'Cancelada pelo solicitante.' });
        BR.toast('Solicitação cancelada.');
        carregar();
      }
    } catch (err) { BR.toast(err.message, true); }
  });
  document.querySelectorAll('[data-fechar]').forEach((b) => b.addEventListener('click', () => b.closest('dialog').close()));

  carregar();
});
