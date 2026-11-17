/* Cadastro e edição de recursos. Capacidades só são definidas no cadastro; depois, via "Disponibilidade". */
BR.ready.then(async (me) => {
  if (!me) { return; }
  const staff = BR.perfilStaff(me);
  const form = document.getElementById('form-recurso');
  const erro = document.getElementById('erro');
  const id = BR.param('id');

  form.categoria.innerHTML = BR.options('categoria');
  const alternarEspecificos = () => {
    document.querySelectorAll('[data-cat]').forEach((g) => { g.hidden = g.dataset.cat !== form.categoria.value; });
  };
  form.categoria.addEventListener('change', alternarEspecificos);

  if (staff) {
    document.getElementById('bloco-org').hidden = false;
    try {
      const orgs = await BR.api.get('/organizacoes?status=APROVADA&size=100');
      form.organizacaoId.innerHTML = '<option value="">Selecione…</option>' +
        orgs.itens.map((o) => `<option value="${o.id}">${BR.esc(o.nome)}</option>`).join('');
      form.organizacaoId.required = true;
    } catch (e) { BR.mostrarErro(erro, e); }
  } else if (me.organizacaoStatus && me.organizacaoStatus !== 'APROVADA') {
    BR.mostrarErro(erro, { message: 'Sua organização ainda não foi aprovada. Você poderá cadastrar recursos depois da aprovação.' });
  }

  const set = (campo, valor) => {
    const el = form.elements[campo];
    if (!el) { return; }
    if (el.type === 'checkbox') { el.checked = !!valor; } else { el.value = valor == null ? '' : valor; }
  };

  if (id) {
    document.getElementById('titulo-form').textContent = 'Editar recurso';
    try {
      const r = await BR.api.get('/recursos/' + id);
      ['nome', 'categoria', 'descricao', 'endereco', 'latitude', 'longitude', 'capacidadeTotal', 'capacidadeDisponivel',
        'unidadeMedida', 'horarioFuncionamento', 'acessivel', 'acessibilidadeDescricao'].forEach((c) => set(c, r[c]));
      if (staff) { set('organizacaoId', r.organizacaoId); form.organizacaoId.disabled = true; }
      const d = r.detalhes || {};
      if (r.categoria === 'AGUA') { set('potavel', d.potavel); set('tipoFonteAgua', d.tipoFonte); }
      if (r.categoria === 'ABRIGO') { set('aceitaAnimais', d.aceitaAnimais); set('possuiRefeitorio', d.possuiRefeitorio); }
      if (r.categoria === 'ENERGIA') { set('tipoFonteEnergia', d.tipoFonte); set('permiteCarregarDispositivos', d.permiteCarregarDispositivos); }
      if (r.categoria === 'ALIMENTACAO') { set('refrigeracao', d.refrigeracao); set('tipoAlimento', d.tipoAlimento); }
      // Categoria e capacidades não mudam na edição.
      ['categoria', 'capacidadeTotal', 'capacidadeDisponivel'].forEach((c) => { form.elements[c].disabled = true; });
      document.getElementById('aviso-edicao').hidden = false;
      if (!staff) { document.getElementById('aviso-revalidacao').hidden = false; }
    } catch (e) { BR.mostrarErro(erro, e); }
  }
  alternarEspecificos();

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    BR.limparErro(erro);
    const v = (n) => form.elements[n].value.trim();
    const cat = form.categoria.value;
    const detalhes = {};
    if (cat === 'AGUA') { detalhes.potavel = form.potavel.checked; detalhes.tipoFonte = v('tipoFonteAgua') || null; }
    if (cat === 'ABRIGO') { detalhes.aceitaAnimais = form.aceitaAnimais.checked; detalhes.possuiRefeitorio = form.possuiRefeitorio.checked; }
    if (cat === 'ENERGIA') { detalhes.tipoFonte = v('tipoFonteEnergia') || null; detalhes.permiteCarregarDispositivos = form.permiteCarregarDispositivos.checked; }
    if (cat === 'ALIMENTACAO') { detalhes.refrigeracao = form.refrigeracao.checked; detalhes.tipoAlimento = v('tipoAlimento') || null; }

    const corpo = {
      nome: v('nome'), categoria: cat, descricao: v('descricao') || null, endereco: v('endereco'),
      latitude: Number(form.latitude.value), longitude: Number(form.longitude.value),
      capacidadeTotal: Number(form.capacidadeTotal.value), capacidadeDisponivel: Number(form.capacidadeDisponivel.value),
      unidadeMedida: v('unidadeMedida'), horarioFuncionamento: v('horarioFuncionamento') || null,
      acessivel: form.acessivel.checked, acessibilidadeDescricao: v('acessibilidadeDescricao') || null,
      organizacaoId: staff && form.organizacaoId.value ? Number(form.organizacaoId.value) : null,
      detalhes: Object.keys(detalhes).length ? detalhes : null
    };
    const botao = form.querySelector('button[type=submit]');
    botao.disabled = true;
    try {
      if (id) { await BR.api.put('/recursos/' + id, corpo); } else { await BR.api.post('/recursos', corpo); }
      window.location.href = '/pages/recursos.html';
    } catch (err) {
      BR.mostrarErro(erro, err);
      botao.disabled = false;
    }
  });
});
