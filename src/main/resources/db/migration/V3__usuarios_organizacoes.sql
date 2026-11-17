-- Taxonomia de referência das categorias de recurso (definidas no escopo do sistema).
INSERT INTO categoria_recurso (codigo, nome, descricao) VALUES
    ('AGUA',        'Água',        'Pontos de abastecimento e distribuição de água'),
    ('ABRIGO',      'Abrigos',     'Locais de acolhimento temporário'),
    ('ENERGIA',     'Energia',     'Pontos de energia e recarga'),
    ('ALIMENTACAO', 'Alimentação', 'Pontos de distribuição de alimentos'),
    ('COMUNICACAO', 'Comunicação', 'Pontos de comunicação e informação'),
    ('SAUDE',       'Saúde',       'Pontos de atendimento em saúde');

CREATE TABLE organizacao (
    id                BIGSERIAL PRIMARY KEY,
    nome              VARCHAR(160) NOT NULL,
    descricao         TEXT,
    tipo              VARCHAR(30)  NOT NULL
        CHECK (tipo IN ('ONG','ASSOCIACAO','COOPERATIVA','INSTITUICAO_PUBLICA','EMPRESA','OUTRA')),
    contato_email     VARCHAR(160),
    contato_telefone  VARCHAR(30),
    endereco          VARCHAR(300),
    latitude          DOUBLE PRECISION CHECK (latitude BETWEEN -90 AND 90),
    longitude         DOUBLE PRECISION CHECK (longitude BETWEEN -180 AND 180),
    responsavel_nome  VARCHAR(120) NOT NULL,
    status_aprovacao  VARCHAR(20)  NOT NULL DEFAULT 'PENDENTE'
        CHECK (status_aprovacao IN ('PENDENTE','APROVADA','REJEITADA','SUSPENSA')),
    criado_em         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    atualizado_em     TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_organizacao_status ON organizacao (status_aprovacao);

CREATE TABLE usuario (
    id              BIGSERIAL PRIMARY KEY,
    nome            VARCHAR(120) NOT NULL,
    email           VARCHAR(160) NOT NULL,
    senha_hash      VARCHAR(100) NOT NULL,
    telefone        VARCHAR(30),
    perfil          VARCHAR(20)  NOT NULL
        CHECK (perfil IN ('MORADOR','ORGANIZACAO','GESTOR','ADMIN')),
    ativo           BOOLEAN      NOT NULL DEFAULT TRUE,
    organizacao_id  BIGINT REFERENCES organizacao (id),
    criado_em       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    atualizado_em   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_usuario_organizacao CHECK (perfil <> 'ORGANIZACAO' OR organizacao_id IS NOT NULL)
);
CREATE UNIQUE INDEX uk_usuario_email ON usuario (lower(email));
CREATE INDEX idx_usuario_organizacao ON usuario (organizacao_id);
