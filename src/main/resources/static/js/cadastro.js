const form = document.getElementById('form-cadastro');
const erro = document.getElementById('erro');
const bloco = document.getElementById('bloco-org');
const perfil = document.getElementById('perfil');

document.getElementById('tipoOrg').innerHTML = BR.options('tipoOrg');
const alternar = () => {
  const org = perfil.value === 'ORGANIZACAO';
  bloco.hidden = !org;
  bloco.querySelectorAll('[data-req]').forEach((el) => { el.required = org; });
};
perfil.addEventListener('change', alternar);
alternar();

form.addEventListener('submit', async (e) => {
  e.preventDefault();
  BR.limparErro(erro);
  const v = (id) => document.getElementById(id).value.trim();
  const corpo = { nome: v('nome'), email: v('email'), senha: document.getElementById('senha').value,
    telefone: v('telefone') || null, perfil: perfil.value, organizacao: null };
  if (perfil.value === 'ORGANIZACAO') {
    corpo.organizacao = { nome: v('orgNome'), tipo: v('tipoOrg'), descricao: v('orgDescricao') || null,
      contatoEmail: v('orgEmail') || null, contatoTelefone: v('orgTelefone') || null,
      endereco: v('orgEndereco') || null, responsavelNome: v('orgResponsavel') };
  }
  const botao = form.querySelector('button[type=submit]');
  botao.disabled = true;
  try {
    await BR.api.post('/auth/register', corpo);
    window.location.href = '/login.html?cadastro=ok';
  } catch (err) {
    BR.mostrarErro(erro, err);
    botao.disabled = false;
  }
});
