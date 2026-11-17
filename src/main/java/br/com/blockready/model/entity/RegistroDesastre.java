package br.com.blockready.model.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * Ocorrência histórica do Atlas Digital de Desastres (MIDR). A abrangência é MUNICIPAL:
 * uma ocorrência nunca deve ser atribuída diretamente a um bairro ou setor específico.
 */
@Entity
@Table(name = "registro_desastre")
public class RegistroDesastre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String municipio;

    @Column(nullable = false, length = 2)
    private String uf;

    @Column(name = "tipo_desastre", nullable = false, length = 160)
    private String tipoDesastre;

    /** Código da Classificação Brasileira de Desastres (quando presente no dataset). */
    @Column(length = 20)
    private String cobrade;

    @Column(name = "ocorrido_em", nullable = false)
    private LocalDate ocorridoEm;

    @Column(nullable = false)
    private int mortos;

    @Column(nullable = false)
    private int afetados;

    @Column(nullable = false)
    private int desabrigados;

    @Column(nullable = false)
    private int desalojados;

    @Column(name = "prejuizo_total", precision = 16, scale = 2)
    private BigDecimal prejuizoTotal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "importacao_id", nullable = false)
    private ImportacaoDataset importacao;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    protected RegistroDesastre() { }

    public RegistroDesastre(String municipio, String uf, String tipoDesastre, String cobrade, LocalDate ocorridoEm,
            int mortos, int afetados, int desabrigados, int desalojados, BigDecimal prejuizoTotal,
            ImportacaoDataset importacao) {
        this.municipio = municipio;
        this.uf = uf;
        this.tipoDesastre = tipoDesastre;
        this.cobrade = cobrade;
        this.ocorridoEm = ocorridoEm;
        this.mortos = mortos;
        this.afetados = afetados;
        this.desabrigados = desabrigados;
        this.desalojados = desalojados;
        this.prejuizoTotal = prejuizoTotal;
        this.importacao = importacao;
    }

    @PrePersist
    void aoCriar() { this.criadoEm = Instant.now(); }

    public Long getId() { return id; }
    public String getMunicipio() { return municipio; }
    public String getUf() { return uf; }
    public String getTipoDesastre() { return tipoDesastre; }
    public String getCobrade() { return cobrade; }
    public LocalDate getOcorridoEm() { return ocorridoEm; }
    public int getMortos() { return mortos; }
    public int getAfetados() { return afetados; }
    public int getDesabrigados() { return desabrigados; }
    public int getDesalojados() { return desalojados; }
    public BigDecimal getPrejuizoTotal() { return prejuizoTotal; }
    public ImportacaoDataset getImportacao() { return importacao; }
    public Instant getCriadoEm() { return criadoEm; }
}
