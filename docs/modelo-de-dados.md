# Modelo de dados

Estado atual (Etapa 5): além das tabelas das Etapas 1–2 (`territorio`, `categoria_recurso`, `usuario`,
`organizacao`, `recurso_comunitario` com subtipos `ponto_agua`, `abrigo`, `ponto_energia`, `ponto_alimentos`,
`registro_disponibilidade`, `recurso_historico`, `necessidade`, `solicitacao`, `solicitacao_historico`,
`voluntario`, `auditoria_registro`), a Etapa 3 criou `fonte_externa`, `importacao_dataset`, `setor_censitario`,
`registro_meteorologico` e `registro_desastre` (migration V7), e a Etapa 4 criou `cenario_emergencia` e
`indicador_cobertura` (migration V8).

Decisões de modelagem das Etapas 3–4, adaptadas à estrutura real das fontes: o **histórico de importações**
é a própria tabela `importacao_dataset` (uma linha por operação, com totais, status e erros — entidade
`HistoricoImportacao` fundida em `ImportacaoDataset`); o papel de `IndicadorTerritorial` é cumprido por
`setor_censitario` (dados territoriais) e `indicador_cobertura` (indicadores calculados); `EventoEmergencia`
se materializou como `cenario_emergencia` (simulação de planejamento). O diagrama de classes completo está
em [diagrama-classes.md](diagrama-classes.md).

Decisões de modelagem da Etapa 2: Morador, Gestor e Administrador são **perfis** de `usuario` (composição, sem herança);
`RecursoComunitario` usa herança JOINED apenas para água, abrigo, energia e alimentação, que têm atributos próprios;
comunicação e saúde usam a classe base. A localização do recurso guarda latitude/longitude e uma coluna `geom`
(geometry Point 4326, gerada) com índice GiST para o filtro por raio.
Atributos listados são uma proposta inicial e podem mudar durante a modelagem de cada módulo.

Convenções: tabelas em `snake_case` no singular, chave `id BIGSERIAL`, datas em `TIMESTAMPTZ` (UTC),
geometrias em `geometry(..., 4326)` com índice GiST.

| Entidade | Finalidade | Atributos principais (proposta) | Relações |
|---|---|---|---|
| **Usuario** | Pessoa autenticada no sistema | nome, e-mail, hash da senha, perfil (morador, voluntário, gestor), ativo | pertence a 0..1 Organizacao; pode ser Voluntario |
| **Organizacao** | Entidade comunitária ou institucional | nome, tipo, contato, território de atuação | tem N Usuario e N RecursoComunitario |
| **RecursoComunitario** | Ponto que oferece um recurso (poço, abrigo, gerador, posto de saúde etc.) | nome, categoria, localização (`Point`), endereço, capacidade | pertence a CategoriaRecurso, Organizacao e Territorio; tem N RegistroDisponibilidade |
| **CategoriaRecurso** | Classificação: água, abrigo, energia, alimentação, comunicação, saúde | código, nome, descrição | tem N RecursoComunitario (**já existe**) |
| **RegistroDisponibilidade** | Situação de um recurso em um momento | recurso, quantidade/capacidade disponível, unidade, situação, registrado em, registrado por, fonte | pertence a RecursoComunitario e, opcionalmente, a EventoEmergencia |
| **Necessidade** | Demanda identificada em um território | categoria, quantidade estimada, urgência, descrição | pertence a Territorio e EventoEmergencia; gera N Solicitacao |
| **Solicitacao** | Pedido concreto de ajuda ou recurso | necessidade, solicitante, situação, criada em | pertence a Necessidade e Usuario; atendida por Voluntario |
| **Voluntario** | Pessoa disponível para ajudar | usuário, habilidades, disponibilidade, território de atuação | vinculado a Usuario e Territorio |
| **EventoEmergencia** | Contexto de planejamento (enchente, apagão etc.) | tipo, nome, início, horizonte em horas (até 72), situação | agrupa Necessidade, RegistroDisponibilidade e IndicadorCobertura |
| **Territorio** | Área geográfica de planejamento (bairro, quadra) | nome, tipo, geometria (`MultiPolygon`) | contém RecursoComunitario e Necessidade (**já existe**) |
| **IndicadorCobertura** | Resultado calculado de suficiência por território, categoria e evento | território, categoria, evento, valor, método (estratégia usada), calculado em | pertence a Territorio, CategoriaRecurso e EventoEmergencia |
| **FonteExterna** | Origem de dados importados | nome, tipo, URL base, última sincronização | referenciada por RegistroDisponibilidade e RecursoComunitario |

## Pontos de extensão

- **Strategy:** a interface `analytics.strategy.AnalysisStrategy` é o ponto de partida para os algoritmos de
  cobertura (`IndicadorCobertura.método` guarda a chave da estratégia usada).
- **Integrações:** `integration/` receberá os clientes das fontes externas (`FonteExterna`).
- **Geografia:** Hibernate Spatial já está no classpath; o Leaflet entra no frontend junto com o módulo de territórios.
- **Segurança:** `SecurityConfig` exige autenticação em `/api/**` (exceto health). O mecanismo será definido com `Usuario`.
