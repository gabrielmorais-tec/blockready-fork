package br.com.blockready.analytics;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import br.com.blockready.dto.Analytics.ParametrosDemandaRequest;
import br.com.blockready.model.enums.CategoriaRecurso;

/**
 * Parâmetros de demanda resolvidos para um cálculo de cobertura. Os padrões abaixo são premissas
 * de planejamento configuráveis (documentadas no README), não estatísticas oficiais:
 * 3 L de água e 2 refeições por pessoa/dia; abrigo para 30% dos afetados; energia e comunicação
 * para 20% e saúde para 5% dos afetados, no horizonte configurado (até 72 h).
 */
public record ParametrosDemanda(
        BigDecimal aguaLitrosPorPessoaDia,
        BigDecimal refeicoesPorPessoaDia,
        BigDecimal percentualDesabrigados,
        BigDecimal percentualEnergia,
        BigDecimal percentualComunicacao,
        BigDecimal percentualSaude,
        BigDecimal limiarSuficiente,
        BigDecimal limiarParcial) {

    private static final MathContext MC = new MathContext(12, RoundingMode.HALF_UP);
    private static final BigDecimal CEM = BigDecimal.valueOf(100);
    private static final BigDecimal HORAS_DIA = BigDecimal.valueOf(24);

    /** Unidades de medida compatíveis com a escala de cada categoria (comparação sem acentos/minúsculas). */
    private static final Map<CategoriaRecurso, Set<String>> UNIDADES_COMPATIVEIS = Map.of(
            CategoriaRecurso.AGUA, Set.of("l", "litro", "litros", "lt", "lts"),
            CategoriaRecurso.ABRIGO, Set.of("vaga", "vagas", "pessoa", "pessoas", "leito", "leitos"),
            CategoriaRecurso.ALIMENTACAO, Set.of("refeicao", "refeicoes", "marmita", "marmitas"),
            CategoriaRecurso.ENERGIA, Set.of("pessoa", "pessoas", "dispositivo", "dispositivos"),
            CategoriaRecurso.COMUNICACAO, Set.of("pessoa", "pessoas"),
            CategoriaRecurso.SAUDE, Set.of("atendimento", "atendimentos", "pessoa", "pessoas", "leito", "leitos"));

    private static final Map<CategoriaRecurso, String> UNIDADE_PADRAO = Map.of(
            CategoriaRecurso.AGUA, "litros",
            CategoriaRecurso.ABRIGO, "vagas",
            CategoriaRecurso.ALIMENTACAO, "refeições",
            CategoriaRecurso.ENERGIA, "pessoas",
            CategoriaRecurso.COMUNICACAO, "pessoas",
            CategoriaRecurso.SAUDE, "atendimentos");

    public static ParametrosDemanda padrao() {
        return new ParametrosDemanda(BigDecimal.valueOf(3), BigDecimal.valueOf(2), BigDecimal.valueOf(30),
                BigDecimal.valueOf(20), BigDecimal.valueOf(20), BigDecimal.valueOf(5),
                BigDecimal.valueOf(90), BigDecimal.valueOf(50));
    }

    /** Resolve a requisição aplicando os padrões aos campos ausentes. */
    public static ParametrosDemanda de(ParametrosDemandaRequest req) {
        ParametrosDemanda p = padrao();
        if (req == null) {
            return p;
        }
        return new ParametrosDemanda(
                escolher(req.aguaLitrosPorPessoaDia(), p.aguaLitrosPorPessoaDia),
                escolher(req.refeicoesPorPessoaDia(), p.refeicoesPorPessoaDia),
                escolher(req.percentualDesabrigados(), p.percentualDesabrigados),
                escolher(req.percentualEnergia(), p.percentualEnergia),
                escolher(req.percentualComunicacao(), p.percentualComunicacao),
                escolher(req.percentualSaude(), p.percentualSaude),
                escolher(req.limiarSuficiente(), p.limiarSuficiente),
                escolher(req.limiarParcial(), p.limiarParcial));
    }

    private static BigDecimal escolher(BigDecimal valor, BigDecimal padrao) {
        return valor != null ? valor : padrao;
    }

    /** Demanda estimada da categoria para a população afetada no horizonte, na unidade padrão da categoria. */
    public BigDecimal demandaEstimada(CategoriaRecurso categoria, long populacaoAfetada, int horizonteHoras) {
        BigDecimal pessoas = BigDecimal.valueOf(populacaoAfetada);
        BigDecimal dias = BigDecimal.valueOf(horizonteHoras).divide(HORAS_DIA, MC);
        BigDecimal demanda = switch (categoria) {
            case AGUA -> pessoas.multiply(aguaLitrosPorPessoaDia, MC).multiply(dias, MC);
            case ALIMENTACAO -> pessoas.multiply(refeicoesPorPessoaDia, MC).multiply(dias, MC);
            case ABRIGO -> percentualDe(pessoas, percentualDesabrigados);
            case ENERGIA -> percentualDe(pessoas, percentualEnergia);
            case COMUNICACAO -> percentualDe(pessoas, percentualComunicacao);
            case SAUDE -> percentualDe(pessoas, percentualSaude);
        };
        return demanda.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal percentualDe(BigDecimal pessoas, BigDecimal percentual) {
        return pessoas.multiply(percentual, MC).divide(CEM, MC);
    }

    public static boolean unidadeCompativel(CategoriaRecurso categoria, String unidade) {
        if (unidade == null) {
            return false;
        }
        String normalizada = java.text.Normalizer.normalize(unidade.trim().toLowerCase(),
                java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return UNIDADES_COMPATIVEIS.get(categoria).contains(normalizada);
    }

    public static String unidadePadrao(CategoriaRecurso categoria) {
        return UNIDADE_PADRAO.get(categoria);
    }

    public Map<String, BigDecimal> comoMapa() {
        Map<String, BigDecimal> m = new LinkedHashMap<>();
        m.put("aguaLitrosPorPessoaDia", aguaLitrosPorPessoaDia);
        m.put("refeicoesPorPessoaDia", refeicoesPorPessoaDia);
        m.put("percentualDesabrigados", percentualDesabrigados);
        m.put("percentualEnergia", percentualEnergia);
        m.put("percentualComunicacao", percentualComunicacao);
        m.put("percentualSaude", percentualSaude);
        m.put("limiarSuficiente", limiarSuficiente);
        m.put("limiarParcial", limiarParcial);
        return m;
    }
}
