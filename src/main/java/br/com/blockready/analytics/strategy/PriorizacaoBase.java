package br.com.blockready.analytics.strategy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import br.com.blockready.dto.Analytics.CriCategoriaResponse;
import br.com.blockready.dto.Analytics.CriResponse;
import br.com.blockready.dto.Analytics.RecomendacaoResponse;
import br.com.blockready.model.enums.CategoriaRecurso;
import br.com.blockready.model.enums.SituacaoCobertura;

/**
 * Lógica comum das estratégias de priorização. O critério é transparente e explicável:
 * urgência = peso da categoria no cenário × (1 − cobertura limitada a 100%).
 * Categorias sem dados entram com urgência média e recomendação de coleta de informações.
 */
public abstract class PriorizacaoBase implements PriorizacaoStrategy {

    private static final BigDecimal CEM = BigDecimal.valueOf(100);

    /** Peso de cada categoria neste cenário (0 a 1); categorias ausentes recebem 0,3. */
    protected abstract Map<CategoriaRecurso, Double> pesos();

    /** Ação sugerida quando a categoria está com cobertura insuficiente neste cenário. */
    protected abstract String acaoPara(CategoriaRecurso categoria);

    @Override
    public List<RecomendacaoResponse> analyze(CriResponse cri) {
        record Candidata(CriCategoriaResponse categoria, double urgencia) { }

        List<Candidata> candidatas = new ArrayList<>();
        for (CriCategoriaResponse c : cri.categorias()) {
            double peso = pesos().getOrDefault(c.categoria(), 0.3);
            double urgencia;
            if (c.situacao() == SituacaoCobertura.SEM_DADOS) {
                urgencia = peso * 0.5;
            } else {
                BigDecimal cobertura = c.percentual().min(CEM).max(BigDecimal.ZERO);
                urgencia = peso * (1.0 - cobertura.doubleValue() / 100.0);
            }
            if (urgencia > 0.0001) {
                candidatas.add(new Candidata(c, urgencia));
            }
        }
        candidatas.sort(Comparator.comparingDouble(Candidata::urgencia).reversed());

        List<RecomendacaoResponse> recomendacoes = new ArrayList<>();
        int prioridade = 1;
        for (Candidata cand : candidatas) {
            CriCategoriaResponse c = cand.categoria();
            double peso = pesos().getOrDefault(c.categoria(), 0.3);
            if (c.situacao() == SituacaoCobertura.SEM_DADOS) {
                recomendacoes.add(new RecomendacaoResponse(prioridade++, c.categoria(), c.unidade(),
                        null, null,
                        "Levantar dados de " + rotulo(c.categoria()) + " antes de mobilizar recursos.",
                        "Sem dados suficientes para calcular a cobertura (" + motivoSemDados(c) + "). "
                                + "Peso da categoria neste cenário: " + formatarPeso(peso) + "."));
            } else {
                recomendacoes.add(new RecomendacaoResponse(prioridade++, c.categoria(), c.unidade(),
                        c.insuficiencia(), c.percentual(), acaoPara(c.categoria()),
                        "Cobertura de " + c.percentual().setScale(1, RoundingMode.HALF_UP)
                                + "% (déficit de " + c.insuficiencia().stripTrailingZeros().toPlainString()
                                + " " + c.unidade() + "). Peso da categoria neste cenário: "
                                + formatarPeso(peso) + "."));
            }
        }
        return recomendacoes;
    }

    private String motivoSemDados(CriCategoriaResponse c) {
        return c.observacao() != null ? c.observacao() : "dados indisponíveis";
    }

    private String formatarPeso(double peso) {
        return BigDecimal.valueOf(peso).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    protected String rotulo(CategoriaRecurso categoria) {
        return switch (categoria) {
            case AGUA -> "água";
            case ABRIGO -> "abrigos";
            case ENERGIA -> "energia";
            case ALIMENTACAO -> "alimentação";
            case COMUNICACAO -> "comunicação";
            case SAUDE -> "saúde";
        };
    }
}
