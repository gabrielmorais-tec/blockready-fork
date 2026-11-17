package br.com.blockready.analytics.strategy;

import java.util.List;

import br.com.blockready.dto.Analytics.CriResponse;
import br.com.blockready.dto.Analytics.RecomendacaoResponse;

/**
 * Padrão Strategy para a priorização de recursos: cada tipo de emergência pesa as categorias de
 * forma diferente e gera recomendações explicáveis a partir do CRI calculado.
 * As implementações são beans Spring resolvidos pela {@link #key()}.
 */
public interface PriorizacaoStrategy extends AnalysisStrategy<CriResponse, List<RecomendacaoResponse>> {

    /** Nome exibível da estratégia (ex.: "Priorização para enchente"). */
    String titulo();
}
