-- Etapa 3: fontes externas, importações de datasets públicos e dados territoriais/meteorológicos/históricos.
-- As três fontes oficiais (IBGE, CEMADEN e Atlas Digital de Desastres) são importadas por arquivo CSV,
-- mecanismo previsto quando os dados são disponibilizados como arquivos públicos (sem inventar endpoints).

CREATE TABLE fonte_externa (
    id                 BIGSERIAL PRIMARY KEY,
    codigo             VARCHAR(40)  NOT NULL UNIQUE,
    nome               VARCHAR(160) NOT NULL,
    descricao          TEXT,
    url_base           VARCHAR(300),
    -- Abrangência geográfica dos registros: evita atribuir dados além da sua representatividade espacial.
    abrangencia        VARCHAR(40)  NOT NULL
        CHECK (abrangencia IN ('SETOR_CENSITARIO', 'ESTACAO', 'MUNICIPIO')),
    ultima_importacao  TIMESTAMPTZ,
    criado_em          TIMESTAMPTZ NOT NULL DEFAULT now(),
    atualizado_em      TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO fonte_externa (codigo, nome, descricao, url_base, abrangencia) VALUES
    ('IBGE_CENSO',
     'IBGE — Censo Demográfico 2022',
     'População, domicílios e malha dos setores censitários. Base para estimar a demanda por recursos.',
     'https://www.ibge.gov.br/estatisticas/sociais/populacao/22827-censo-demografico-2022.html',
     'SETOR_CENSITARIO'),
    ('CEMADEN_PLUVIO',
     'CEMADEN — Monitoramento pluviométrico',
     'Precipitação observada pelas estações do CEMADEN. Registros valem para a localização da estação.',
     'https://www.gov.br/cemaden/pt-br',
     'ESTACAO'),
    ('ATLAS_DESASTRES',
     'Atlas Digital de Desastres — MIDR',
     'Histórico de desastres com abrangência municipal: ocorrências não devem ser atribuídas a bairros específicos.',
     'https://atlasdigital.mdr.gov.br/',
     'MUNICIPIO');

-- Importação de dataset: registro e histórico de cada operação (a lista é o próprio histórico de importações).
CREATE TABLE importacao_dataset (
    id                 BIGSERIAL PRIMARY KEY,
    fonte_id           BIGINT       NOT NULL REFERENCES fonte_externa (id),
    nome_arquivo       VARCHAR(260) NOT NULL,
    hash_arquivo       CHAR(64)     NOT NULL,
    periodo_referencia VARCHAR(60),
    -- Dados demonstrativos devem ser explicitamente identificados em toda a interface.
    demonstrativo      BOOLEAN      NOT NULL DEFAULT FALSE,
    status             VARCHAR(30)  NOT NULL
        CHECK (status IN ('CONCLUIDA', 'CONCLUIDA_COM_REJEICOES', 'FALHA')),
    total_processados  INT          NOT NULL DEFAULT 0,
    total_rejeitados   INT          NOT NULL DEFAULT 0,
    erros              TEXT,
    importado_por_id   BIGINT       NOT NULL REFERENCES usuario (id),
    criado_em          TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_importacao_fonte ON importacao_dataset (fonte_id, criado_em DESC);
-- Controle de duplicidade: o mesmo arquivo (hash) não entra duas vezes na mesma fonte.
CREATE UNIQUE INDEX ux_importacao_fonte_hash ON importacao_dataset (fonte_id, hash_arquivo)
    WHERE status <> 'FALHA';

-- IBGE: setor censitário com população, domicílios e (opcionalmente) a malha geográfica.
CREATE TABLE setor_censitario (
    id             BIGSERIAL PRIMARY KEY,
    codigo_setor   VARCHAR(25)  NOT NULL UNIQUE,
    municipio      VARCHAR(160) NOT NULL,
    uf             CHAR(2)      NOT NULL,
    populacao      INT          NOT NULL CHECK (populacao >= 0),
    domicilios     INT          NOT NULL CHECK (domicilios >= 0),
    area_km2       NUMERIC(12,4) CHECK (area_km2 >= 0),
    -- SRID 4326 (WGS 84), o mesmo dos recursos comunitários; malha opcional via WKT.
    geometria      geometry(MultiPolygon, 4326),
    importacao_id  BIGINT       NOT NULL REFERENCES importacao_dataset (id),
    criado_em      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    atualizado_em  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_setor_geometria ON setor_censitario USING GIST (geometria);
CREATE INDEX idx_setor_municipio ON setor_censitario (municipio, uf);

-- CEMADEN: precipitação observada por estação (horários em UTC; valem para o ponto da estação).
CREATE TABLE registro_meteorologico (
    id               BIGSERIAL PRIMARY KEY,
    estacao_codigo   VARCHAR(40)  NOT NULL,
    estacao_nome     VARCHAR(160),
    municipio        VARCHAR(160) NOT NULL,
    uf               CHAR(2)      NOT NULL,
    latitude         DOUBLE PRECISION NOT NULL CHECK (latitude BETWEEN -90 AND 90),
    longitude        DOUBLE PRECISION NOT NULL CHECK (longitude BETWEEN -180 AND 180),
    geom             geometry(Point, 4326)
        GENERATED ALWAYS AS (ST_SetSRID(ST_MakePoint(longitude, latitude), 4326)) STORED,
    medido_em        TIMESTAMPTZ  NOT NULL,
    precipitacao_mm  NUMERIC(8,2) NOT NULL CHECK (precipitacao_mm >= 0),
    importacao_id    BIGINT       NOT NULL REFERENCES importacao_dataset (id),
    criado_em        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    -- Duplicidade: uma medição por estação e instante.
    CONSTRAINT ux_meteo_estacao_hora UNIQUE (estacao_codigo, medido_em)
);
CREATE INDEX idx_meteo_geom ON registro_meteorologico USING GIST (geom);
CREATE INDEX idx_meteo_medido ON registro_meteorologico (medido_em DESC);

-- Atlas Digital de Desastres (MIDR): histórico de eventos com abrangência MUNICIPAL.
CREATE TABLE registro_desastre (
    id              BIGSERIAL PRIMARY KEY,
    municipio       VARCHAR(160) NOT NULL,
    uf              CHAR(2)      NOT NULL,
    tipo_desastre   VARCHAR(160) NOT NULL,
    cobrade         VARCHAR(20),
    ocorrido_em     DATE         NOT NULL,
    mortos          INT          NOT NULL DEFAULT 0 CHECK (mortos >= 0),
    afetados        INT          NOT NULL DEFAULT 0 CHECK (afetados >= 0),
    desabrigados    INT          NOT NULL DEFAULT 0 CHECK (desabrigados >= 0),
    desalojados     INT          NOT NULL DEFAULT 0 CHECK (desalojados >= 0),
    prejuizo_total  NUMERIC(16,2) CHECK (prejuizo_total >= 0),
    importacao_id   BIGINT       NOT NULL REFERENCES importacao_dataset (id),
    criado_em       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    -- Duplicidade: um evento por município, tipo e data.
    CONSTRAINT ux_desastre UNIQUE (municipio, uf, tipo_desastre, ocorrido_em)
);
CREATE INDEX idx_desastre_tipo ON registro_desastre (tipo_desastre);
CREATE INDEX idx_desastre_data ON registro_desastre (ocorrido_em DESC);
