-- Ajuste de tipos: colunas CHAR(n) viram VARCHAR(n) para alinhar com o mapeamento JPA (String -> VARCHAR),
-- evitando falha na validação de schema do Hibernate. Migration incremental, sem perda de dados.

ALTER TABLE importacao_dataset ALTER COLUMN hash_arquivo TYPE VARCHAR(64);
ALTER TABLE setor_censitario ALTER COLUMN uf TYPE VARCHAR(2);
ALTER TABLE registro_meteorologico ALTER COLUMN uf TYPE VARCHAR(2);
ALTER TABLE registro_desastre ALTER COLUMN uf TYPE VARCHAR(2);
