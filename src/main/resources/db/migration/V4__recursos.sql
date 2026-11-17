CREATE TABLE recurso_comunitario (
    id                        BIGSERIAL PRIMARY KEY,
    nome                      VARCHAR(160) NOT NULL,
    categoria                 VARCHAR(40)  NOT NULL REFERENCES categoria_recurso (codigo),
    descricao                 TEXT,
    endereco                  VARCHAR(300) NOT NULL,
    latitude                  DOUBLE PRECISION NOT NULL CHECK (latitude BETWEEN -90 AND 90),
    longitude                 DOUBLE PRECISION NOT NULL CHECK (longitude BETWEEN -180 AND 180),
    -- Coluna geográfica derivada de latitude/longitude, usada nos filtros por raio.
    geom                      geometry(Point, 4326)
        GENERATED ALWAYS AS (ST_SetSRID(ST_MakePoint(longitude, latitude), 4326)) STORED,
    capacidade_total          NUMERIC(12,2) NOT NULL CHECK (capacidade_total >= 0),
    capacidade_disponivel     NUMERIC(12,2) NOT NULL CHECK (capacidade_disponivel >= 0),
    unidade_medida            VARCHAR(30)  NOT NULL,
    horario_funcionamento     VARCHAR(200),
    acessivel                 BOOLEAN      NOT NULL DEFAULT FALSE,
    acessibilidade_descricao  VARCHAR(300),
    status                    VARCHAR(25)  NOT NULL DEFAULT 'PENDENTE_VALIDACAO'
        CHECK (status IN ('PENDENTE_VALIDACAO','VALIDADO','REJEITADO','INATIVO')),
    organizacao_id            BIGINT       NOT NULL REFERENCES organizacao (id),
    validado_por_id           BIGINT       REFERENCES usuario (id),
    validado_em               TIMESTAMPTZ,
    criado_em                 TIMESTAMPTZ  NOT NULL DEFAULT now(),
    atualizado_em             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_recurso_disponivel CHECK (capacidade_disponivel <= capacidade_total)
);
CREATE INDEX idx_recurso_geom ON recurso_comunitario USING GIST (geom);
CREATE INDEX idx_recurso_categoria_status ON recurso_comunitario (categoria, status);
CREATE INDEX idx_recurso_organizacao ON recurso_comunitario (organizacao_id);

-- Subtipos (herança JOINED): cada tabela guarda só os atributos específicos.
CREATE TABLE ponto_agua (
    id          BIGINT PRIMARY KEY REFERENCES recurso_comunitario (id),
    potavel     BOOLEAN NOT NULL DEFAULT FALSE,
    tipo_fonte  VARCHAR(80)
);
CREATE TABLE abrigo (
    id                BIGINT PRIMARY KEY REFERENCES recurso_comunitario (id),
    aceita_animais    BOOLEAN NOT NULL DEFAULT FALSE,
    possui_refeitorio BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE TABLE ponto_energia (
    id                            BIGINT PRIMARY KEY REFERENCES recurso_comunitario (id),
    tipo_fonte                    VARCHAR(80),
    permite_carregar_dispositivos BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE TABLE ponto_alimentos (
    id             BIGINT PRIMARY KEY REFERENCES recurso_comunitario (id),
    refrigeracao   BOOLEAN NOT NULL DEFAULT FALSE,
    tipo_alimento  VARCHAR(80)
);

-- Histórico de capacidade e disponibilidade.
CREATE TABLE registro_disponibilidade (
    id                     BIGSERIAL PRIMARY KEY,
    recurso_id             BIGINT NOT NULL REFERENCES recurso_comunitario (id),
    capacidade_total       NUMERIC(12,2) NOT NULL CHECK (capacidade_total >= 0),
    capacidade_disponivel  NUMERIC(12,2) NOT NULL CHECK (capacidade_disponivel >= 0),
    observacao             VARCHAR(500),
    registrado_por_id      BIGINT NOT NULL REFERENCES usuario (id),
    registrado_em          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_registro_disponivel CHECK (capacidade_disponivel <= capacidade_total)
);
CREATE INDEX idx_registro_disp_recurso ON registro_disponibilidade (recurso_id, registrado_em DESC);

-- Histórico de alterações do recurso (criação, edição, validação, desativação).
CREATE TABLE recurso_historico (
    id          BIGSERIAL PRIMARY KEY,
    recurso_id  BIGINT NOT NULL REFERENCES recurso_comunitario (id),
    acao        VARCHAR(40) NOT NULL,
    descricao   VARCHAR(500),
    usuario_id  BIGINT NOT NULL REFERENCES usuario (id),
    criado_em   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_recurso_hist_recurso ON recurso_historico (recurso_id, criado_em DESC);
