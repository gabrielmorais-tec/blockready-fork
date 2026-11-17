package br.com.blockready.model.entity;

import java.math.BigDecimal;
import java.time.Instant;

import br.com.blockready.model.enums.CategoriaRecurso;
import br.com.blockready.model.enums.SituacaoCobertura;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * Resultado persistido do Community Resilience Index para uma categoria:
 * CRI = (capacidade disponível / demanda estimada) × 100, com método (estratégia) e fonte dos dados.
 */
@Entity
@Table(name = "indicador_cobertura")
public class IndicadorCobertura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cenario_id")
    private CenarioEmergencia cenario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private CategoriaRecurso categoria;

    @Column(name = "capacidade_disponivel", nullable = false, precision = 14, scale = 2)
    private BigDecimal capacidadeDisponivel;

    @Column(name = "demanda_estimada", nullable = false, precision = 14, scale = 2)
    private BigDecimal demandaEstimada;

    @Column(precision = 8, scale = 2)
    private BigDecimal percentual;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SituacaoCobertura situacao;

    @Column(nullable = false, length = 30)
    private String unidade;

    @Column(name = "fonte_dados", nullable = false, length = 300)
    private String fonteDados;

    @Column(nullable = false, length = 60)
    private String metodo;

    @Column(name = "calculado_em", nullable = false, updatable = false)
    private Instant calculadoEm;

    protected IndicadorCobertura() { }

    public IndicadorCobertura(CenarioEmergencia cenario, CategoriaRecurso categoria,
            BigDecimal capacidadeDisponivel, BigDecimal demandaEstimada, BigDecimal percentual,
            SituacaoCobertura situacao, String unidade, String fonteDados, String metodo) {
        this.cenario = cenario;
        this.categoria = categoria;
        this.capacidadeDisponivel = capacidadeDisponivel;
        this.demandaEstimada = demandaEstimada;
        this.percentual = percentual;
        this.situacao = situacao;
        this.unidade = unidade;
        this.fonteDados = fonteDados;
        this.metodo = metodo;
    }

    @PrePersist
    void aoCriar() { this.calculadoEm = Instant.now(); }

    public Long getId() { return id; }
    public CenarioEmergencia getCenario() { return cenario; }
    public CategoriaRecurso getCategoria() { return categoria; }
    public BigDecimal getCapacidadeDisponivel() { return capacidadeDisponivel; }
    public BigDecimal getDemandaEstimada() { return demandaEstimada; }
    public BigDecimal getPercentual() { return percentual; }
    public SituacaoCobertura getSituacao() { return situacao; }
    public String getUnidade() { return unidade; }
    public String getFonteDados() { return fonteDados; }
    public String getMetodo() { return metodo; }
    public Instant getCalculadoEm() { return calculadoEm; }
}
