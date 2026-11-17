/* Monta navegação lateral e cabeçalho em volta de <main id="content"> e garante a autenticação. */
(function () {
  window.BR = window.BR || {};
  const TODOS = ['MORADOR', 'ORGANIZACAO', 'GESTOR', 'ADMIN'];
  const STAFF = ['GESTOR', 'ADMIN'];
  const NAV = [
    { id: 'painel', label: 'Painel', href: '/index.html', perfis: TODOS, icon: 'M3 3h8v8H3zM13 3h8v5h-8zM13 11h8v10h-8zM3 13h8v8H3z' },
    { id: 'recursos', label: 'Recursos', href: '/pages/recursos.html', perfis: TODOS, icon: 'M12 3 3 8l9 5 9-5-9-5zM3 13l9 5 9-5' },
    { id: 'necessidades', label: 'Necessidades', href: '/pages/necessidades.html', perfis: ['MORADOR'], icon: 'M12 3 2 20h20L12 3zM12 10v4M12 17.5h.01' },
    { id: 'solicitacoes', label: 'Solicitações', href: '/pages/solicitacoes.html', perfis: TODOS, icon: 'M9 5h10M9 12h10M9 19h10M4 5h.01M4 12h.01M4 19h.01' },
    { id: 'organizacoes', label: 'Organizações', href: '/pages/organizacoes.html', perfis: ['ORGANIZACAO'].concat(STAFF), icon: 'M4 21V7l8-4 8 4v14M9 21v-6h6v6' },
    { id: 'voluntarios', label: 'Voluntários', href: '/pages/voluntarios.html', perfis: TODOS, icon: 'M9 11a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7zM2.5 20a6.5 6.5 0 0 1 13 0M16 4.5a3.5 3.5 0 0 1 0 6.5M18 14a6.5 6.5 0 0 1 3.5 6' },
    { id: 'territorios', label: 'Territórios', href: '/pages/territorios.html', perfis: STAFF, icon: 'M12 21s-7-6.2-7-11a7 7 0 1 1 14 0c0 4.8-7 11-7 11zM12 12.5a2.5 2.5 0 1 0 0-5 2.5 2.5 0 0 0 0 5z' },
    { id: 'indicadores', label: 'Indicadores', href: '/pages/indicadores.html', perfis: STAFF, icon: 'M5 20V10M12 20V4M19 20v-7' }
  ];

  const content = document.getElementById('content');
  const publica = document.body.dataset.public === 'true';
  BR.publicPage = publica;

  let resolver;
  BR.ready = new Promise((res) => { resolver = res; });

  if (!content || publica) { resolver(null); return; }

  const svg = (d) => `<svg viewBox="0 0 24 24" aria-hidden="true"><path d="${d}"/></svg>`;

  async function iniciar() {
    let me;
    try {
      me = await BR.api.get('/auth/me');
    } catch (e) {
      return; // 401 já redireciona para o login
    }
    BR.me = me;
    const pagina = document.body.dataset.page;
    const titulo = document.body.dataset.title || 'BLOCKREADY';

    const sidebar = document.createElement('aside');
    sidebar.className = 'sidebar';
    sidebar.id = 'sidebar';
    sidebar.innerHTML = `
      <a class="brand" href="/index.html"><img src="/assets/logo.svg" alt=""><strong>BLOCKREADY</strong></a>
      <nav aria-label="Principal"><ul class="nav">
        ${NAV.filter((n) => n.perfis.includes(me.perfil)).map((n) =>
          `<li><a href="${n.href}"${n.id === pagina ? ' aria-current="page"' : ''}>${svg(n.icon)}<span>${n.label}</span></a></li>`).join('')}
      </ul></nav>
      <div class="sidebar-foot">Stronger Neighborhoods.<br>Safer Futures.<br><a href="/components/guia-componentes.html">Guia de componentes</a></div>`;

    const header = document.createElement('header');
    header.className = 'header';
    header.innerHTML = `
      <button class="btn btn-icon menu-btn" type="button" aria-controls="sidebar" aria-expanded="false" aria-label="Abrir menu">${svg('M4 6h16M4 12h16M4 18h16')}</button>
      <h1>${BR.esc(titulo)}</h1><span class="spacer"></span>
      <span class="badge badge-info" title="${BR.esc(me.email)}">${BR.esc(me.nome)} · ${BR.esc(BR.rotulo('perfil', me.perfil))}</span>
      <button class="btn btn-secondary btn-sm" type="button" id="btn-sair">Sair</button>`;

    const scrim = document.createElement('div');
    scrim.className = 'scrim';
    const mainCol = document.createElement('div');
    mainCol.className = 'main-col';
    const app = document.createElement('div');
    app.className = 'app';

    document.body.insertBefore(app, content);
    mainCol.append(header, content);
    app.append(sidebar, mainCol);
    document.body.append(scrim);
    content.hidden = false;

    const btn = header.querySelector('.menu-btn');
    const alternar = (abrir) => {
      sidebar.classList.toggle('is-open', abrir);
      scrim.classList.toggle('is-open', abrir);
      btn.setAttribute('aria-expanded', String(abrir));
    };
    btn.addEventListener('click', () => alternar(!sidebar.classList.contains('is-open')));
    scrim.addEventListener('click', () => alternar(false));
    document.addEventListener('keydown', (e) => { if (e.key === 'Escape') { alternar(false); } });

    document.getElementById('btn-sair').addEventListener('click', async () => {
      try { await BR.api.post('/auth/logout'); } finally { window.location.href = '/login.html'; }
    });

    resolver(me);
  }
  iniciar();
})();
