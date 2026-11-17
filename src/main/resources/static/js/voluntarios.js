/* Voluntários: qualquer usuário cadastra a própria disponibilidade; gestores consultam a lista. */
BR.ready.then(async (me) => {
  if (!me) { return; }
  const form = document.getElementById('form-vol');
  const erro = document.getElementById('erro');

  try {
    const v = await BR.api.get('/voluntarios/me');
    form.habilidades.value = v.habilidades;
    form.disponibilidade.value = v.disponibilidade;
    form.ativo.checked = v.ativo;
  } catch (e) {
    if (e.status !== 404) { BR.mostrarErro(erro, e); }
    form.ativo.checked = true;
  }

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    BR.limparErro(erro);
    try {
      await BR.api.put('/voluntarios/me', { habilidades: form.habilidades.value.trim(),
        disponibilidade: form.disponibilidade.value.trim(), ativo: form.ativo.checked });
      BR.toast('Disponibilidade salva.');
    } catch (err) { BR.mostrarErro(erro, err); }
  });

  if (BR.perfilStaff(me)) {
    document.getElementById('painel-staff').hidden = false;
    try {
      const p = await BR.api.get('/voluntarios?size=50');
      document.getElementById('lista').innerHTML = p.itens.length ? p.itens.map((v) => `<tr>
        <td><strong>${BR.esc(v.nome)}</strong><span class="sub">${BR.esc(v.telefone || '')}</span></td>
        <td>${BR.esc(v.habilidades)}</td><td>${BR.esc(v.disponibilidade)}</td>
        <td>${v.ativo ? '<span class="badge badge-ok">Ativo</span>' : '<span class="badge badge-neutral">Inativo</span>'}</td></tr>`).join('')
        : '<tr><td colspan="4">Nenhum voluntário cadastrado.</td></tr>';
    } catch (e) { BR.mostrarErro(document.getElementById('erro-lista'), e); }
  }
});
