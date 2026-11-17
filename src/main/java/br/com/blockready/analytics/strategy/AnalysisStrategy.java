package br.com.blockready.analytics.strategy;

/**
 * Contrato base do padrão Strategy para as análises do BLOCKREADY
 * (por exemplo, cálculo de cobertura de recursos por território).
 *
 * <p>Cada algoritmo será uma implementação registrada como bean Spring; o serviço de análise
 * escolhe a estratégia pela {@link #key()}. Nenhuma implementação existe nesta etapa.</p>
 *
 * @param <I> tipo da entrada da análise
 * @param <O> tipo do resultado
 */
public interface AnalysisStrategy<I, O> {

    /** Identificador único da estratégia (ex.: {@code "cobertura-por-populacao"}). */
    String key();

    O analyze(I input);
}
