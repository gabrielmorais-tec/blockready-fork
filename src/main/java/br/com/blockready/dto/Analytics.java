package br.com.blockready.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import br.com.blockready.model.enums.CategoriaRecurso;
import br.com.blockready.model.enums.SituacaoCobertura;
import br.com.blockready.model.enums.TipoEmergencia;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Requisições e respostas do módulo de inteligência analítica (CRI, insights e cenários). */
public final class Analytics {

    private Analytics() { }

    /**
     * Parâmetros de demanda configuráveis. Valores nulos assumem os padrões de planejamento
     * documentados (premissas configuráveis, não dados oficiais).
     */
    public record ParametrosDemandaRequest(
            @DecimalMin("0") @DecimalMax("100") BigDecimal aguaLitrosPorPessoaDia,
            @DecimalMin("0") @DecimalMax("20") BigDecimal refeicoesPorPessoaDia,
            @DecimalMin("0") @DecimalMax("100") BigDecimal percentualDesabrigados,
            @DecimalMin("0") @DecimalMax("100") BigDecimal percentualEnergia,
            @DecimalMin("0") @DecimalMax("100") BigDecimal percentualComunicacao,
            @DecimalMin("0") @DecimalMax("100") BigDecimal percentualSaude,
            @DecimalMin("1") @DecimalMax("200") BigDecimal limiarSuficiente,
            @DecimalMin("0") @DecimalMax("199") BigDecimal limiarParcial) { }

    /** Cobertura de uma categoria: CRI = capacidade disponível / demanda estimada × 100. */
    public record CriCategoriaResponse(CategoriaRecurso categoria, String unidade,
            BigDecimal capacidadeDisponivel, BigDecimal demandaEstimada, BigDecimal percentual,
            SituacaoCobertura situacao, BigDecimal insuficiencia, int recursosConsiderados,
            int recursosExcluidosPorUnidade, String fonteDados, Instant ultimaAtualizacaoRecurso,
            String observacao) { }

    public record CriResponse(Instant calculadoEm, long populacaoBase, long setoresConsiderados,
            BigDecimal percentualAfetados, long populacaoAfetada, int horizonteHoras, String metodo,
            Map<String, BigDecimal> parametros, List<CriCategoriaResponse> categorias,
            List<String> fontes, List<String> limitacoes, boolean dadosSuficientes) { }

    public record RecomendacaoResponse(int prioridade, CategoriaRecurso categoria, String unidade,
            BigDecimal deficit, BigDecimal cobertura, String acao, String justificativa) { }

    public record CenarioRequest(
            @NotBlank @Size(max = 160) String nome,
            @NotNull TipoEmergencia tipo,
            @NotNull @Min(1) @Max(72) Integer horizonteHoras,
            @NotNull @DecimalMin("0.1") @DecimalMax("100") BigDecimal percentualAfetados,
            List<CategoriaRecurso> categorias,
            ParametrosDemandaRequest parametros) { }

    public record CenarioResponse(Long id, String nome, TipoEmergencia tipo, int horizonteHoras,
            BigDecimal percentualAfetados, String estrategia, String criadoPorNome, Instant criadoEm,
            CriResponse cri, List<RecomendacaoResponse> recomendacoes, List<String> avisos) { }

    public record CenarioResumoResponse(Long id, String nome, TipoEmergencia tipo, int horizonteHoras,
            BigDecimal percentualAfetados, String estrategia, String criadoPorNome, Instant criadoEm) { }

    /** Estrutura comum dos quatro insights estratégicos, com explicabilidade completa. */
    public record InsightResponse(String codigo, String titulo, String perguntaEstrategica,
            List<String> dadosUtilizados, List<String> fontes, String metodologia, boolean dadosSuficientes,
            Object resultado, String interpretacao, String decisaoRecomendada, String impactoPotencial,
            List<String> limitacoes, Instant geradoEm) { }
}
