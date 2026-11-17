package br.com.blockready.integration;

import java.util.List;

import br.com.blockready.model.entity.ImportacaoDataset;

/**
 * Contrato dos importadores de datasets públicos. Cada fonte (IBGE, CEMADEN, Atlas) tem uma
 * implementação própria, registrada como bean e resolvida pelo código da fonte — a lógica de
 * integração fica fora dos controllers.
 */
public interface ImportadorDataset {

    /** Resultado do processamento: totais e erros linha a linha (limitados para não estourar o registro). */
    record Resultado(int processados, int rejeitados, List<String> erros) { }

    /** Código da {@link br.com.blockready.model.entity.FonteExterna} que este importador atende. */
    String fonteCodigo();

    /** Colunas aceitas, para a tela de importação e a documentação. */
    String colunasEsperadas();

    /** Valida o cabeçalho; lança {@link br.com.blockready.exception.BusinessException} se incompatível. */
    void validarCabecalho(CsvArquivo csv);

    Resultado importar(CsvArquivo csv, ImportacaoDataset importacao);
}
