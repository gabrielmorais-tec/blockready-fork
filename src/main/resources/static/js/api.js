/* Cliente da API REST (/api/v1) e utilitários compartilhados. */
window.BR = window.BR || {};

(function () {
  const BASE = '/api/v1';

  function cookie(nome) {
    const m = document.cookie.match(new RegExp('(?:^|; )' + nome + '=([^;]*)'));
    return m ? decodeURIComponent(m[1]) : null;
  }

  class ApiError extends Error {
    constructor(status, problema) {
      super((problema && (problema.detail || problema.title)) || 'Não foi possível concluir a operação.');
      this.status = status;
      this.campos = (problema && problema.campos) || null;
    }
  }

  async function request(metodo, caminho, corpo) {
    const headers = { Accept: 'application/json' };
    if (corpo !== undefined) { headers['Content-Type'] = 'application/json'; }
    if (metodo !== 'GET') {
      const token = cookie('XSRF-TOKEN');
      if (token) { headers['X-XSRF-TOKEN'] = token; }
    }
    const res = await fetch(BASE + caminho, {
      method: metodo, headers, credentials: 'same-origin',
      body: corpo !== undefined ? JSON.stringify(corpo) : undefined
    });
    if (res.status === 204) { return null; }
    let dados = null;
    try { dados = await res.json(); } catch (_) { /* sem corpo JSON */ }
    if (res.status === 401 && !BR.publicPage && caminho !== '/auth/login') {
      window.location.href = '/login.html';
    }
    if (!res.ok) { throw new ApiError(res.status, dados); }
    return dados;
  }

  BR.ApiError = ApiError;
  BR.api = {
    get: (c) => request('GET', c),
    post: (c, b) => request('POST', c, b),
    put: (c, b) => request('PUT', c, b),
    patch: (c, b) => request('PATCH', c, b),
    del: (c) => request('DELETE', c),
    /** Retorna { ok, status, body } sem lançar; usado no health check. */
    async raw(c) {
      const res = await fetch(BASE + c, { headers: { Accept: 'application/json' }, credentials: 'same-origin' });
      let body = null;
      try { body = await res.json(); } catch (_) { /* sem corpo */ }
      return { ok: res.ok, status: res.status, body };
    }
  };

  /** Escapa texto antes de inserir em innerHTML. */
  BR.esc = (v) => String(v == null ? '' : v)
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;').replace(/'/g, '&#39;');

  BR.data = (iso) => (iso ? new Date(iso).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' }) : '—');
  BR.num = (n) => (n == null ? '—' : Number(n).toLocaleString('pt-BR'));

  BR.labels = {
    categoria: { AGUA: 'Água', ABRIGO: 'Abrigos', ENERGIA: 'Energia', ALIMENTACAO: 'Alimentação', COMUNICACAO: 'Comunicação', SAUDE: 'Saúde' },
    perfil: { MORADOR: 'Morador', ORGANIZACAO: 'Organização parceira', GESTOR: 'Gestor comunitário', ADMIN: 'Administrador' },
    prioridade: { BAIXA: 'Baixa', MEDIA: 'Média', ALTA: 'Alta', CRITICA: 'Crítica' },
    statusRecurso: { PENDENTE_VALIDACAO: 'Pendente de validação', VALIDADO: 'Confirmado', REJEITADO: 'Rejeitado', INATIVO: 'Inativo' },
    statusSolicitacao: { ABERTA: 'Aberta', EM_ANALISE: 'Em análise', EM_ATENDIMENTO: 'Em atendimento', ATENDIDA: 'Atendida', CANCELADA: 'Cancelada' },
    aprovacao: { PENDENTE: 'Pendente', APROVADA: 'Aprovada', REJEITADA: 'Rejeitada', SUSPENSA: 'Suspensa' },
    tipoOrg: { ONG: 'ONG', ASSOCIACAO: 'Associação', COOPERATIVA: 'Cooperativa', INSTITUICAO_PUBLICA: 'Instituição pública', EMPRESA: 'Empresa', OUTRA: 'Outra' }
  };
  BR.rotulo = (grupo, chave) => (BR.labels[grupo] && BR.labels[grupo][chave]) || chave || '—';

  const CLASSES = {
    VALIDADO: 'ok', APROVADA: 'ok', ATENDIDA: 'ok',
    PENDENTE_VALIDACAO: 'alert', PENDENTE: 'alert', ABERTA: 'alert', ALTA: 'alert',
    CRITICA: 'danger', REJEITADO: 'danger', REJEITADA: 'danger', SUSPENSA: 'danger',
    EM_ANALISE: 'info', EM_ATENDIMENTO: 'info', MEDIA: 'info',
    INATIVO: 'neutral', CANCELADA: 'neutral', BAIXA: 'neutral'
  };
  BR.badge = (grupo, chave) =>
    `<span class="badge badge-${CLASSES[chave] || 'neutral'}">${BR.esc(BR.rotulo(grupo, chave))}</span>`;

  BR.options = (grupo, vazio) => (vazio ? `<option value="">${vazio}</option>` : '') +
    Object.entries(BR.labels[grupo]).map(([k, v]) => `<option value="${k}">${v}</option>`).join('');

  BR.toast = (msg, erro) => {
    const el = document.createElement('div');
    el.className = 'toast' + (erro ? ' erro' : '');
    el.setAttribute('role', erro ? 'alert' : 'status');
    el.textContent = msg;
    document.body.append(el);
    setTimeout(() => el.remove(), 4500);
  };

  /** Mostra erros de validação (campo -> mensagem) em um contêiner. */
  BR.mostrarErro = (el, erro) => {
    if (!el) { return; }
    const extras = erro.campos ? '<ul>' + Object.entries(erro.campos).map(([c, m]) => `<li><strong>${BR.esc(c)}</strong>: ${BR.esc(m)}</li>`).join('') + '</ul>' : '';
    el.innerHTML = `<div class="alert-erro" role="alert">${BR.esc(erro.message)}${extras}</div>`;
  };
  BR.limparErro = (el) => { if (el) { el.innerHTML = ''; } };

  BR.param = (nome) => new URLSearchParams(window.location.search).get(nome);
  BR.perfilStaff = (me) => me && (me.perfil === 'GESTOR' || me.perfil === 'ADMIN');
})();
