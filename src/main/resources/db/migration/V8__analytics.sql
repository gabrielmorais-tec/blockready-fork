-- Etapa 4: inteligência analítica — cenários de emergência (planejamento 72h) e indicadores de cobertura (CRI).

CREATE TABLE cenario_emergencia (
    id                    BIGSERIAL PRIMARY KEY,
    nome                  VARCHAR(160) NOT NULL,
    tipo                  VARCHAR(40)  NOT NULL
        CHECK (tipo IN ('ENCHENTE', 'TEMPESTADE_INTENSA', 'INTERRUPCAO_ENERGIA', 'INTERRUPCAO_SERVICOS')),
    -- Horizonte de planejamento configurável, limitado às 72 horas da proposta.
    horizonte_horas       INT          NOT NULL CHECK (horizonte_horas BETWEEN 1 AND 72),
    percentual_afetados   NUMERIC(5,2) NOT NULL CHECK (percentual_afetados BETWEEN 0 AND 100),
    estrategia            VARCHAR(60)  NOT NULL,
    -- Parâmetros de demanda e resultado completo da simulação, serializados em JSON.
    parametros            TEXT         NOT NULL,
    resultado             TEXT         NOT NULL,
    criado_por_id         BIGINT       NOT NULL REFERENCES usuario (id),
    criado_em             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    atualizado_em         TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_cenario_criado ON cenario_emergencia (criado_em DESC);

-- Indicador de cobertura por categoria (CRI = capacidade disponível / demanda estimada × 100).
-- Com cenario_id nulo, é um cálculo corrente do painel; preenchido, pertence a uma simulação salva.
CREATE TABLE indicador_cobertura (
    id                     BIGSERIAL PRIMARY KEY,
    cenario_id             BIGINT REFERENCES cenario_emergencia (id) ON DELETE CASCADE,
    categoria              VARCHAR(40)  NOT NULL REFERENCES categoria_recurso (codigo),
    capacidade_disponivel  NUMERIC(14,2) NOT NULL CHECK (capacidade_disponivel >= 0),
    demanda_estimada       NUMERIC(14,2) NOT NULL CHECK (demanda_estimada >= 0),
    -- Nulo quando o cálculo não é possível (demanda zero ou dados insuficientes).
    percentual             NUMERIC(8,2) CHECK (percentual >= 0),
    situacao               VARCHAR(30)  NOT NULL
        CHECK (situacao IN ('SUFICIENTE', 'PARCIAL', 'INSUFICIENTE', 'SEM_DADOS')),
    unidade                VARCHAR(30)  NOT NULL,
    fonte_dados            VARCHAR(300) NOT NULL,
    metodo                 VARCHAR(60)  NOT NULL,
    calculado_em           TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_indicador_cenario ON indicador_cobertura (cenario_id);
CREATE INDEX idx_indicador_categoria ON indicador_cobertura (categoria, calculado_em DESC);
