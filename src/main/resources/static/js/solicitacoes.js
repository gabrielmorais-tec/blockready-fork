/* Gestão de solicitações: consulta, atualização de status com histórico e registro do atendimento. */
BR.ready.then((me) => {
  if (!me) { return; }
  const f = document.getElementById('filtros');
  const corpo = document.getElementById('lista');
  const erro = document.getElementById('erro');
  const dlgStatus = document.getElementById('dlg-status');
  const formStatus = document.getElementById('form-status');
  const dlgHist = document.getElementById('dlg-hist');
  let pagina = 0;
  let itens = [];
  let alvo = null;
  let novoStatus = null;

  f.status.innerHTML = BR.options('statusSolicitacao', 'Todos');
  f.categoria.innerHTML = BR.options('categoria', 'Todas');
  f.prioridade.innerHTML = BR.options('prioridade', 'Todas');

  const permitidos = (s) => (me.perfil === 'MORADOR' ? s.proximosStatus.filter((x) => x === 'CANCELADA') : s.proximosStatus);

  function linha(s) {
    const botoes = permitidos(s).map((p) =>
      `<button class="btn ${p === 'CANCELADA' ? 'btn-secondary' : 'btn-primary'} btn-sm" data-act="status" data-id="${s.id}" data-status="${p}">${BR.esc(p === 'CANCELADA' ? 'Cancelar' : '→ ' + BR.rotulo('statusSolicitacao', p))}</button>`).join('');
    const local = s.endereco ? `<span class="sub">${BR.esc(s.endereco)}</span>` : '<span class="sub">Endereço visível apenas ao responsável</span>';
    return `<tr>
      <td>#${s.id}<span class="sub">${BR.data(s.criadoEm)}</span></td>
      <td>${BR.esc(BR.rotulo('categoria', s.categoria))}</td>
      <td>${BR.esc(s.descricao)}<span class="sub">${BR.num(s.quantidade)} ${BR.esc(s.unidade)}</span>${local}</td>
      <td>${BR.badge('prioridade', s.prioridade)}</td>
      <td>${BR.badge('statusSolicitacao', s.status)}${s.responsavelNome ? `<span class="sub">Resp.: ${BR.esc(s.responsavelNome)}</span>` : ''}${s.registroAtendimento ? `<span class="sub">${BR.esc(s.registroAtendimento)}</span>` : ''}</td>
      <td><div class="actions"><button class="btn btn-secondary btn-sm" data-act="hist" data-id="${s.id}">Histórico</button>${botoes}</div></td></tr>`;
  }

  async function carregar() {
    BR.limparErro(erro);
    const q = new URLSearchParams({ page: pagina, size: 15 });
    ['status', 'categoria', 'prioridade'].forEach((k) => { if (f[k].value) { q.set(k, f[k].value); } });
    try {
      const p = await BR.api.get('/solicitacoes?' + q);
      itens = p.itens;
      corpo.innerHTML = itens.length ? itens.map(linha).join('') : '<tr><td colspan="6">Nenhuma solicitação encontrada.</td></tr>';
      document.getElementById('pager').innerHTML = `<span>${BR.num(p.total)} solicitação(ões) · página ${p.pagina + 1} de ${Math.max(p.totalPaginas, 1)}</span>
        <button class="btn btn-secondary btn-sm" id="ant" ${p.pagina === 0 ? 'disabled' : ''}>Anterior</button>
        <button class="btn btn-secondary btn-sm" id="prox" ${p.pagina + 1 >= p.totalPaginas ? 'disabled' : ''}>Próxima</button>`;
      document.getElementById('ant').onclick = () => { pagina--; carregar(); };
      document.getElementById('prox').onclick = () => { pagina++; carregar(); };
    } catch (e) { BR.mostrarErro(erro, e); }
  }

  f.addEventListener('submit', (e) => { e.preventDefault(); pagina = 0; carregar(); });
  document.getElementById('limpar').addEventListener('click', () => { f.reset(); pagina = 0; carregar(); });

  corpo.addEventListener('click', async (e) => {
    const b = e.target.closest('[data-act]');
    if (!b) { return; }
    try {
      if (b.dataset.act === 'hist') {
        const h = await BR.api.get(`/solicitacoes/${b.dataset.id}/historico`);
        document.getElementById('hist-lista').innerHTML = h.map((x) =>
          `<li><strong>${BR.esc(BR.rotulo('statusSolicitacao', x.statusNovo))}</strong>${x.observacao ? ' — ' + BR.esc(x.observacao) : ''}<span class="when">${BR.data(x.criadoEm)}${x.usuarioNome ? ' · ' + BR.esc(x.usuarioNome) : ''}</span></li>`).join('');
        dlgHist.showModal();
      } else {
        alvo = b.dataset.id;
        novoStatus = b.dataset.status;
        document.getElementById('status-titulo').textContent = 'Mover para: ' + BR.rotulo('statusSolicitacao', novoStatus);
        document.getElementById('bloco-atendimento').hidden = novoStatus !== 'ATENDIDA';
        formStatus.registro.required = novoStatus === 'ATENDIDA';
        formStatus.reset();
        BR.limparErro(document.getElementById('erro-status'));
        dlgStatus.showModal();
      }
    } catch (err) { BR.toast(err.message, true); }
  });

  formStatus.addEventListener('submit', async (e) => {
    e.preventDefault();
    try {
      await BR.api.patch(`/solicitacoes/${alvo}/status`, {
        status: novoStatus, observacao: formStatus.observacao.value.trim() || null,
        registroAtendimento: novoStatus === 'ATENDIDA' ? formStatus.registro.value.trim() : null
      });
      dlgStatus.close();
      BR.toast('Status atualizado.');
      carregar();
    } catch (err) { BR.mostrarErro(document.getElementById('erro-status'), err); }
  });
  document.querySelectorAll('[data-fechar]').forEach((b) => b.addEventListener('click', () => b.closest('dialog').close()));

  carregar();
});
