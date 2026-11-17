package br.com.blockready.model.entity;

import java.math.BigDecimal;

import br.com.blockready.model.enums.TipoEmergencia;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Simulação de cenário de emergência salva para comparação. Guarda os parâmetros usados e o
 * resultado calculado (JSON), lembrando que é uma ferramenta de planejamento, não uma previsão.
 */
@Entity
@Table(name = "cenario_emergencia")
public class CenarioEmergencia extends EntidadeBase {

    @Column(nullable = false, length = 160)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TipoEmergencia tipo;

    @Column(name = "horizonte_horas", nullable = false)
    private int horizonteHoras;

    @Column(name = "percentual_afetados", nullable = false, precision = 5, scale = 2)
    private BigDecimal percentualAfetados;

    @Column(nullable = false, length = 60)
    private String estrategia;

    @Column(nullable = false, columnDefinition = "text")
    private String parametros;

    @Column(nullable = false, columnDefinition = "text")
    private String resultado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "criado_por_id", nullable = false)
    private Usuario criadoPor;

    protected CenarioEmergencia() { }

    public CenarioEmergencia(String nome, TipoEmergencia tipo, int horizonteHoras,
            BigDecimal percentualAfetados, String estrategia, String parametros, String resultado,
            Usuario criadoPor) {
        this.nome = nome;
        this.tipo = tipo;
        this.horizonteHoras = horizonteHoras;
        this.percentualAfetados = percentualAfetados;
        this.estrategia = estrategia;
        this.parametros = parametros;
        this.resultado = resultado;
        this.criadoPor = criadoPor;
    }

    public String getNome() { return nome; }
    public TipoEmergencia getTipo() { return tipo; }
    public int getHorizonteHoras() { return horizonteHoras; }
    public BigDecimal getPercentualAfetados() { return percentualAfetados; }
    public String getEstrategia() { return estrategia; }
    public String getParametros() { return parametros; }
    public String getResultado() { return resultado; }
    public Usuario getCriadoPor() { return criadoPor; }
}
