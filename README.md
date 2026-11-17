# BLOCKREADY

**Stronger Neighborhoods. Safer Futures.**

Plataforma CitizenTech de resiliência comunitária: conecta moradores, organizações, voluntários e gestores para
mapear recursos essenciais (água, abrigos, energia, alimentação, comunicação e saúde), identificar insuficiências
e apoiar o planejamento durante emergências, com horizonte de até 72 horas.

> O BLOCKREADY é uma ferramenta de apoio ao planejamento. Não garante a sobrevivência nem a segurança de uma comunidade.

**Estado: Etapa 2 de 5 (sistema central).** Autenticação por perfil, recursos comunitários, necessidades e solicitações, organizações e voluntários estão implementados. Territórios e Indicadores seguem como placeholders. Datasets externos, CRI e insights não fazem parte desta etapa.

## Tecnologias

Java 25 · Spring Boot 3.5 (MVC, Data JPA, Security, Validation, Actuator) · Maven · PostgreSQL 16 + PostGIS 3.4 ·
Flyway · Hibernate Spatial · JUnit 5 · HTML5/CSS3/JavaScript puro · Docker Compose · Leaflet (etapas futuras).

## Pré-requisitos

- **Via Docker (recomendado):** Docker com Docker Compose v2.
- **Via Maven local:** JDK 25, Maven 3.9+ e um PostgreSQL com PostGIS (o Compose pode subir só o banco).

## Execução com Docker

```bash
cp .env.example .env        # edite e defina DB_PASSWORD
docker compose up --build
```

Acesse http://localhost:8080 (ou a porta de `APP_PORT`).

## Execução local (Maven)

```bash
cp .env.example .env        # defina DB_PASSWORD
docker compose up -d db     # sobe só o PostgreSQL/PostGIS

# Linux/macOS
set -a; source .env; set +a
mvn spring-boot:run
```

No Windows (PowerShell), defina as variáveis com `$env:DB_PASSWORD="..."` antes de `mvn spring-boot:run`.

## Verificação

```bash
curl -i http://localhost:8080/api/v1/health
```

Resposta esperada (HTTP 200): `status: "UP"`, banco `UP` e a versão do PostGIS. Se o banco ou o PostGIS falharem, a
resposta é HTTP 503 com `status: "DOWN"`. O endpoint padrão `/actuator/health` também está disponível.

Testes:

```bash
mvn test      # unitários (sem banco nem Docker)
mvn verify    # inclui FluxoComunitarioIT, que usa Testcontainers (exige Docker)
```

## Variáveis de ambiente

| Variável | Padrão | Descrição |
|---|---|---|
| `DB_PASSWORD` | — (obrigatória) | Senha do PostgreSQL |
| `BOOTSTRAP_ADMIN_EMAIL` / `BOOTSTRAP_ADMIN_PASSWORD` | vazio | Cria o primeiro administrador se ainda não existir nenhum (senha de 8 a 72 caracteres). Sem elas, nenhum admin é criado |
| `SESSION_COOKIE_SECURE` | `false` | Defina `true` quando servir por HTTPS |
| `DB_NAME` / `DB_USER` | `blockready` | Banco e usuário |
| `DB_HOST` / `DB_PORT` | `localhost` / `5432` | Endereço do banco (no Compose, `DB_HOST` é `db`) |
| `APP_PORT` | `8080` | Porta HTTP da aplicação |
| `LOG_LEVEL_ROOT` / `LOG_LEVEL_APP` | `INFO` | Níveis de log |
| `LOG_DIR` | `logs` | Pasta dos logs em arquivo (rotação diária/10 MB, 14 dias) |

Nenhuma credencial é versionada: `.env` está no `.gitignore`.

## Estrutura

```
src/main/java/br/com/blockready/
  config  controller  service  repository  model/entity  dto  mapper
  exception  security  integration  analytics/strategy
src/main/resources/
  application.yml  logback-spring.xml  db/migration/
  static/ (assets, css, js, pages, components)
docs/modelo-de-dados.md      # entidades previstas
```

Arquitetura em camadas (MVC). Os repositories Spring Data JPA exercem o papel de DAO.
`analytics/strategy/AnalysisStrategy` prepara o padrão Strategy.

## Banco de dados e migrations

Flyway aplica `db/migration` na inicialização: `V1` PostGIS, `V2` tabelas-base (Etapa 1), `V3` usuários e organizações
(e as 6 categorias de referência), `V4` recursos, subtipos e histórico de disponibilidade, `V5` necessidades, solicitações
e voluntários, `V6` auditoria. Modelo completo em [docs/modelo-de-dados.md](docs/modelo-de-dados.md).

## Primeiro acesso

1. Defina `BOOTSTRAP_ADMIN_EMAIL` e `BOOTSTRAP_ADMIN_PASSWORD` no `.env` e suba a aplicação.
2. Entre em `/login.html` com esse administrador e crie contas de **gestor** (`POST /api/v1/usuarios`).
3. Moradores e organizações se cadastram em `/cadastro.html`. Uma organização só cadastra recursos depois de **aprovada** por um gestor.

## Segurança

- **Autenticação:** sessão HTTP (cookie `JSESSIONID`, HttpOnly, SameSite=Lax) com troca do id da sessão no login. Senhas com BCrypt (custo 12), entre 8 e 72 caracteres.
- **CSRF:** o cookie `XSRF-TOKEN` é lido pelo JavaScript e enviado em `X-XSRF-TOKEN` nas requisições que alteram dados. Chamadas externas (curl) precisam enviar os dois.
- **Autorização:** `@PreAuthorize` por perfil nos controllers e regra de dono nos serviços (uma organização só altera os próprios recursos).
- **Dados pessoais:** endereço e nome do solicitante só aparecem para o próprio morador, para gestores/administradores e para a organização responsável pelo atendimento. Quem validou um recurso só é exibido a gestores, administradores e à organização dona.
- **Auditoria:** validação/rejeição/desativação de recursos, decisões sobre organizações, mudanças de perfil e de ativação, atribuição de responsável e atendimentos. Consulta apenas por administradores.
- **Fora desta etapa:** limite de tentativas de login, recuperação de senha e tela de administração de usuários (a API existe).

## Perfis

| Perfil | Pode |
|---|---|
| `MORADOR` | Criar conta, consultar recursos confirmados, registrar necessidades, acompanhar e cancelar as próprias solicitações, cadastrar disponibilidade como voluntário |
| `ORGANIZACAO` | Gerir os dados da própria organização, cadastrar recursos (após aprovação), editar, atualizar disponibilidade e desativar **apenas os seus** recursos, consultar solicitações da categoria dos seus recursos e atendê-las |
| `GESTOR` | Validar/rejeitar recursos, aprovar/rejeitar/suspender organizações, gerenciar qualquer recurso, consultar e atualizar solicitações, atribuir responsáveis, listar voluntários |
| `ADMIN` | Tudo do gestor, mais usuários e permissões e registros de auditoria |

## Regras de negócio

- Recurso novo nasce **PENDENTE_VALIDACAO** e não é tratado como confirmado. Fluxo: `PENDENTE_VALIDACAO → VALIDADO | REJEITADO`; `INATIVO` por desativação. Se uma organização edita os dados de um recurso já validado, ele volta a pendente.
- Capacidade disponível nunca excede a total; cada atualização gera um registro em `registro_disponibilidade`; as alterações ficam em `recurso_historico`.
- `DELETE /recursos/{id}` **desativa** (não apaga), preservando histórico.
- Solicitação: `ABERTA → EM_ANALISE → EM_ATENDIMENTO → ATENDIDA`, com `CANCELADA` possível até o atendimento. `ATENDIDA` exige o registro do atendimento. Cada transição grava `solicitacao_historico`.
- Moradores só podem cancelar as próprias solicitações.

## Endpoints REST

Prefixo `/api/v1`. Erros seguem RFC 7807 (`400` validação com `campos`, `401`, `403`, `404`, `409`, `422` regra de negócio). Listas são paginadas (`page`, `size` ≤ 100).

**Autenticação**

| Método e rota | Acesso | Descrição |
|---|---|---|
| `POST /auth/register` | público | Cria conta `MORADOR` ou `ORGANIZACAO` (esta com objeto `organizacao`) |
| `POST /auth/login` | público | Autentica e abre a sessão |
| `POST /auth/logout` | autenticado | Encerra a sessão (204) |
| `GET /auth/me` | autenticado | Usuário atual |

**Usuários (ADMIN)**: `GET /usuarios` · `POST /usuarios` · `PATCH /usuarios/{id}/perfil` · `PATCH /usuarios/{id}/ativo`

**Organizações**

| Método e rota | Acesso |
|---|---|
| `GET /organizacoes?status=` | GESTOR, ADMIN |
| `GET /organizacoes/minha` | ORGANIZACAO |
| `GET /organizacoes/{id}` | dona, GESTOR, ADMIN |
| `PUT /organizacoes/{id}` | dona, GESTOR, ADMIN |
| `POST /organizacoes/{id}/aprovar` · `/rejeitar` · `/suspender` | GESTOR, ADMIN |

**Recursos**

| Método e rota | Acesso |
|---|---|
| `GET /recursos?categoria=&status=&busca=&lat=&lon=&raioKm=` | autenticado (escopo por perfil) |
| `GET /recursos/{id}` | autenticado (pendentes só para staff e dona) |
| `POST /recursos` | ORGANIZACAO (aprovada), GESTOR, ADMIN |
| `PUT /recursos/{id}` | dona, GESTOR, ADMIN |
| `PATCH /recursos/{id}/disponibilidade` | dona, GESTOR, ADMIN |
| `POST /recursos/{id}/validar` · `/rejeitar` | GESTOR, ADMIN |
| `DELETE /recursos/{id}` (desativa) | dona, GESTOR, ADMIN |
| `GET /recursos/{id}/historico` · `/disponibilidades` | dona, GESTOR, ADMIN |

O filtro por localização usa PostGIS (`ST_DWithin`); `lat`, `lon` e `raioKm` (até 200) devem vir juntos.

**Necessidades e solicitações**

| Método e rota | Acesso |
|---|---|
| `POST /necessidades` | MORADOR (abre a solicitação) |
| `GET /solicitacoes?status=&categoria=&prioridade=` | autenticado (morador: as próprias; organização: da categoria dos seus recursos ou sob sua responsabilidade; staff: todas) |
| `GET /solicitacoes/{id}` · `/historico` | quem pode ver a solicitação |
| `PATCH /solicitacoes/{id}/status` | morador (só cancelar), organização, GESTOR, ADMIN |
| `PATCH /solicitacoes/{id}/responsavel` | GESTOR, ADMIN |

**Voluntários**: `GET /voluntarios/me` · `PUT /voluntarios/me` (qualquer usuário) · `GET /voluntarios` (GESTOR, ADMIN)

**Outros**: `GET /dashboard` (contagens lidas do banco) · `GET /auditoria` (ADMIN) · `GET /health` (público)

## Telas

`/login.html`, `/cadastro.html`, `/index.html` (painel), `/pages/recursos.html`, `/pages/recurso-form.html`, `/pages/necessidades.html`, `/pages/solicitacoes.html`, `/pages/organizacoes.html`, `/pages/voluntarios.html`. A navegação muda conforme o perfil e todas consomem a API real.
