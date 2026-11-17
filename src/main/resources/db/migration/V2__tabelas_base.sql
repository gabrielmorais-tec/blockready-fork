-- Tabelas-base da fundação. As demais entidades estão documentadas em docs/modelo-de-dados.md
-- e entram em migrations próprias (V3, V4, ...) nas próximas etapas.

CREATE TABLE territorio (
    id             BIGSERIAL PRIMARY KEY,
    nome           VARCHAR(160) NOT NULL,
    tipo           VARCHAR(40)  NOT NULL,
    geometria      geometry(MultiPolygon, 4326),
    criado_em      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    atualizado_em  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_territorio_geometria ON territorio USING GIST (geometria);

CREATE TABLE categoria_recurso (
    id         BIGSERIAL PRIMARY KEY,
    codigo     VARCHAR(40)  NOT NULL UNIQUE,
    nome       VARCHAR(80)  NOT NULL,
    descricao  TEXT,
    criado_em  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
