CREATE TABLE necessidade (
    id          BIGSERIAL PRIMARY KEY,
    morador_id  BIGINT NOT NULL REFERENCES usuario (id),
    categoria   VARCHAR(40) NOT NULL REFERENCES categoria_recurso (codigo),
    descricao   TEXT NOT NULL,
    quantidade  NUMERIC(12,2) NOT NULL CHECK (quantidade > 0),
    unidade     VARCHAR(30) NOT NULL,
    endereco    VARCHAR(300) NOT NULL,
    latitude    DOUBLE PRECISION CHECK (latitude BETWEEN -90 AND 90),
    longitude   DOUBLE PRECISION CHECK (longitude BETWEEN -180 AND 180),
    prioridade  VARCHAR(10) NOT NULL CHECK (prioridade IN ('BAIXA','MEDIA','ALTA','CRITICA')),
    criado_em   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_necessidade_morador ON necessidade (morador_id);
CREATE INDEX idx_necessidade_categoria ON necessidade (categoria);

CREATE TABLE solicitacao (
    id                    BIGSERIAL PRIMARY KEY,
    necessidade_id        BIGINT NOT NULL UNIQUE REFERENCES necessidade (id),
    status                VARCHAR(20) NOT NULL DEFAULT 'ABERTA'
        CHECK (status IN ('ABERTA','EM_ANALISE','EM_ATENDIMENTO','ATENDIDA','CANCELADA')),
    prioridade            VARCHAR(10) NOT NULL CHECK (prioridade IN ('BAIXA','MEDIA','ALTA','CRITICA')),
    responsavel_id        BIGINT REFERENCES usuario (id),
    registro_atendimento  TEXT,
    atendida_em           TIMESTAMPTZ,
    criado_em             TIMESTAMPTZ NOT NULL DEFAULT now(),
    atualizado_em         TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_solicitacao_atendida CHECK (status <> 'ATENDIDA' OR registro_atendimento IS NOT NULL)
);
CREATE INDEX idx_solicitacao_status ON solicitacao (status);
CREATE INDEX idx_solicitacao_responsavel ON solicitacao (responsavel_id);

CREATE TABLE solicitacao_historico (
    id               BIGSERIAL PRIMARY KEY,
    solicitacao_id   BIGINT NOT NULL REFERENCES solicitacao (id),
    status_anterior  VARCHAR(20),
    status_novo      VARCHAR(20) NOT NULL,
    observacao       VARCHAR(1000),
    usuario_id       BIGINT NOT NULL REFERENCES usuario (id),
    criado_em        TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_solicitacao_hist ON solicitacao_historico (solicitacao_id, criado_em);

CREATE TABLE voluntario (
    id             BIGSERIAL PRIMARY KEY,
    usuario_id     BIGINT NOT NULL UNIQUE REFERENCES usuario (id),
    habilidades    VARCHAR(1000) NOT NULL,
    disponibilidade VARCHAR(500) NOT NULL,
    ativo          BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em      TIMESTAMPTZ NOT NULL DEFAULT now(),
    atualizado_em  TIMESTAMPTZ NOT NULL DEFAULT now()
);
