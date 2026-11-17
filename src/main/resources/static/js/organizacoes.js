/* Organizações: gestores aprovam/rejeitam/suspendem; a organização edita apenas os próprios dados. */
BR.ready.then(async (me) => {
  if (!me) { return; }
  const erro = document.getElementById('erro');
  const staff = BR.perfilStaff(me);

  if (staff) {
    document.getElementById('painel-staff').hidden = false;
    const filtro = document.getElementById('filtro-status');
    filtro.innerHTML = BR.options('aprovacao', 'Todas');
    const corpo = document.getElementById('lista');
    let itens = [];

    async function carregar() {
      BR.limparErro(erro);
      const q = new URLSearchParams({ size: 50 });
      if (filtro.value) { q.set('status', filtro.value); }
      try {
        const p = await BR.api.get('/organizacoes?' + q);
        itens = p.itens;
        corpo.innerHTML = itens.length ? itens.map((o) => `<tr>
          <td><strong>${BR.esc(o.nome)}</strong><span class="sub">${BR.esc(BR.rotulo('tipoOrg', o.tipo))}</span></td>
          <td>${BR.esc(o.responsavelNome)}<span class="sub">${BR.esc(o.contatoEmail || '')} ${BR.esc(o.contatoTelefone || '')}</span></td>
          <td>${BR.esc(o.endereco || '—')}</td>
          <td>${BR.badge('aprovacao', o.statusAprovacao)}</td>
          <td><div class="actions">
            ${o.statusAprovacao !== 'APROVADA' ? `<button class="btn btn-primary btn-sm" data-act="aprovar" data-id="${o.id}">Aprovar</button>` : ''}
            ${o.statusAprovacao === 'PENDENTE' ? `<button class="btn btn-alert btn-sm" data-act="rejeitar" data-id="${o.id}">Rejeitar</button>` : ''}
            ${o.statusAprovacao === 'APROVADA' ? `<button class="btn btn-secondary btn-sm" data-act="suspender" data-id="${o.id}">Suspender</button>` : ''}
          </div></td></tr>`).join('') : '<tr><td colspan="5">Nenhuma organização encontrada.</td></tr>';
      } catch (e) { BR.mostrarErro(erro, e); }
    }
    filtro.addEventListener('change', carregar);
    corpo.addEventListener('click', async (e) => {
      const b = e.target.closest('[data-act]');
      if (!b) { return; }
      try {
        await BR.api.post(`/organizacoes/${b.dataset.id}/${b.dataset.act}`);
        BR.toast('Decisão registrada.');
        carregar();
      } catch (err) { BR.toast(err.message, true); }
    });
    carregar();
    return;
  }

  // Organização parceira: dados institucionais próprios
  const form = document.getElementById('form-org');
  document.getElementById('painel-org').hidden = false;
  form.tipo.innerHTML = BR.options('tipoOrg');
  let org;
  try {
    org = await BR.api.get('/organizacoes/minha');
  } catch (e) { BR.mostrarErro(erro, e); return; }
  ['nome', 'descricao', 'tipo', 'contatoEmail', 'contatoTelefone', 'endereco', 'latitude', 'longitude', 'responsavelNome']
    .forEach((c) => { form.elements[c].value = org[c] == null ? '' : org[c]; });
  document.getElementById('status-org').innerHTML = BR.badge('aprovacao', org.statusAprovacao);
  if (org.statusAprovacao !== 'APROVADA') { document.getElementById('aviso-aprovacao').hidden = false; }

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    BR.limparErro(erro);
    const v = (n) => form.elements[n].value.trim();
    try {
      await BR.api.put('/organizacoes/' + org.id, {
        nome: v('nome'), descricao: v('descricao') || null, tipo: v('tipo'), contatoEmail: v('contatoEmail') || null,
        contatoTelefone: v('contatoTelefone') || null, endereco: v('endereco') || null,
        latitude: v('latitude') ? Number(v('latitude')) : null, longitude: v('longitude') ? Number(v('longitude')) : null,
        responsavelNome: v('responsavelNome')
      });
      BR.toast('Dados atualizados.');
    } catch (err) { BR.mostrarErro(erro, err); }
  });
});
