const form = document.getElementById('form-login');
const erro = document.getElementById('erro');
if (BR.param('cadastro') === 'ok') {
  document.getElementById('aviso').hidden = false;
}
form.addEventListener('submit', async (e) => {
  e.preventDefault();
  BR.limparErro(erro);
  const botao = form.querySelector('button[type=submit]');
  botao.disabled = true;
  try {
    await BR.api.post('/auth/login', { email: form.email.value, senha: form.senha.value });
    window.location.href = '/index.html';
  } catch (err) {
    BR.mostrarErro(erro, err.status === 401 ? new BR.ApiError(401, { detail: 'E-mail ou senha inválidos.' }) : err);
    botao.disabled = false;
  }
});
