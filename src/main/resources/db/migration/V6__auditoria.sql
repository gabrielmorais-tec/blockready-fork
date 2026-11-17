CREATE TABLE auditoria_registro (
    id           BIGSERIAL PRIMARY KEY,
    usuario_id   BIGINT REFERENCES usuario (id),
    acao         VARCHAR(60)  NOT NULL,
    entidade     VARCHAR(60)  NOT NULL,
    entidade_id  BIGINT,
    detalhes     VARCHAR(1000),
    criado_em    TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_auditoria_criado_em ON auditoria_registro (criado_em DESC);
CREATE INDEX idx_auditoria_entidade ON auditoria_registro (entidade, entidade_id);
