/* Administração: usuários, permissões e auditoria (perfil ADMIN). */
BR.ready.then(async (me) => {
  if (!me || me.perfil !== 'ADMIN') { return; }

  document.getElementById('n-perfil').innerHTML = BR.options('perfil');
  let pagUsuarios = 0;
  let pagAuditoria = 0;

  function pager(el, r, pagina, aoMudar) {
    el.innerHTML = r.totalPaginas > 1
      ? `<button class="btn btn-secondary btn-sm" data-dir="-1" ${pagina === 0 ? 'disabled' : ''}>Anterior</button>
         <span>Página ${r.pagina + 1} de ${r.totalPaginas}</span>
         <button class="btn btn-secondary btn-sm" data-dir="1" ${pagina + 1 >= r.totalPaginas ? 'disabled' : ''}>Próxima</button>`
      : '';
    el.querySelectorAll('[data-dir]').forEach((b) =>
      b.addEventListener('click', () => aoMudar(pagina + Number(b.dataset.dir))));
  }

  async function carregarUsuarios() {
    const el = document.getElementById('lista-usuarios');
    try {
      const r = await BR.api.get(`/usuarios?page=${pagUsuarios}&size=15`);
      el.innerHTML = r.itens.map((u) => `
        <tr>
          <td>${BR.esc(u.nome)}</td>
          <td>${BR.esc(u.email)}</td>
          <td>
            <select class="select" data-perfil="${u.id}" ${u.id === me.id ? 'disabled title="Você não altera o próprio perfil"' : ''}>
              ${Object.entries(BR.labels.perfil).map(([k, v]) =>
                `<option value="${k}" ${k === u.perfil ? 'selected' : ''}>${v}</option>`).join('')}
            </select>
          </td>
          <td>${u.ativo ? '<span class="badge badge-ok">Ativo</span>' : '<span class="badge badge-danger">Inativo</span>'}</td>
          <td>${BR.data(u.criadoEm)}</td>
          <td><button class="btn btn-secondary btn-sm" data-ativo="${u.id}" data-valor="${!u.ativo}"
                ${u.id === me.id ? 'disabled title="Você não desativa a própria conta"' : ''}>
                ${u.ativo ? 'Desativar' : 'Reativar'}</button></td>
        </tr>`).join('');

      el.querySelectorAll('[data-perfil]').forEach((s) => s.addEventListener('change', async () => {
        try {
          await BR.api.patch(`/usuarios/${s.dataset.perfil}/perfil`, { perfil: s.value });
          BR.toast('Perfil atualizado.');
        } catch (e) {
          BR.mostrarErro(document.getElementById('erro-usuarios'), e);
        }
        carregarUsuarios();
      }));
      el.querySelectorAll('[data-ativo]').forEach((b) => b.addEventListener('click', async () => {
        try {
          await BR.api.patch(`/usuarios/${b.dataset.ativo}/ativo`, { ativo: b.dataset.valor === 'true' });
          BR.toast('Situação atualizada.');
        } catch (e) {
          BR.mostrarErro(document.getElementById('erro-usuarios'), e);
        }
        carregarUsuarios();
      }));
      pager(document.getElementById('pager-usuarios'), r, pagUsuarios, (p) => { pagUsuarios = p; carregarUsuarios(); });
    } catch (e) {
      el.innerHTML = '<tr><td colspan="6">Não foi possível carregar.</td></tr>';
      BR.mostrarErro(document.getElementById('erro-usuarios'), e);
    }
  }

  document.getElementById('form-novo').addEventListener('submit', async (ev) => {
    ev.preventDefault();
    const erro = document.getElementById('erro-novo');
    BR.limparErro(erro);
    try {
      await BR.api.post('/usuarios', {
        nome: document.getElementById('n-nome').value.trim(),
        email: document.getElementById('n-email').value.trim(),
        senha: document.getElementById('n-senha').value,
        telefone: document.getElementById('n-telefone').value || null,
        perfil: document.getElementById('n-perfil').value
      });
      BR.toast('Usuário criado.');
      document.getElementById('form-novo').reset();
      carregarUsuarios();
    } catch (e) {
      BR.mostrarErro(erro, e);
    }
  });

  async function carregarAuditoria() {
    const el = document.getElementById('lista-auditoria');
    try {
      const r = await BR.api.get(`/auditoria?page=${pagAuditoria}&size=15`);
      el.innerHTML = r.itens.length
        ? r.itens.map((a) => `
            <tr>
              <td>${BR.data(a.criadoEm)}</td>
              <td>${a.usuarioId == null ? '—' : BR.num(a.usuarioId)}</td>
              <td>${BR.esc(a.acao)}</td>
              <td>${BR.esc(a.entidade)}${a.entidadeId != null ? ' #' + a.entidadeId : ''}</td>
              <td class="hint">${BR.esc(a.detalhes || '—')}</td>
            </tr>`).join('')
        : '<tr><td colspan="5">Nenhum registro ainda.</td></tr>';
      pager(document.getElementById('pager-auditoria'), r, pagAuditoria, (p) => { pagAuditoria = p; carregarAuditoria(); });
    } catch (e) {
      el.innerHTML = '<tr><td colspan="5">Não foi possível carregar.</td></tr>';
      BR.mostrarErro(document.getElementById('erro-auditoria'), e);
    }
  }

  await Promise.all([carregarUsuarios(), carregarAuditoria()]);
});
