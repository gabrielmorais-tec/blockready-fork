-- A importação é registrada antes de processar as linhas (os registros filhos referenciam o pai):
-- o status inicial PROCESSANDO entra no conjunto permitido.
ALTER TABLE importacao_dataset DROP CONSTRAINT importacao_dataset_status_check;
ALTER TABLE importacao_dataset ADD CONSTRAINT importacao_dataset_status_check
    CHECK (status IN ('PROCESSANDO', 'CONCLUIDA', 'CONCLUIDA_COM_REJEICOES', 'FALHA'));
