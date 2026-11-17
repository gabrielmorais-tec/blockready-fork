package br.com.blockready.model.entity;

import java.math.BigDecimal;
import java.time.Instant;

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
 * Medição pluviométrica do CEMADEN, armazenada em UTC. A medição vale para o ponto da estação;
 * a coluna geográfica {@code geom} é gerada pelo banco a partir de latitude/longitude.
 */
@Entity
@Table(name = "registro_meteorologico")
public class RegistroMeteorologico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "estacao_codigo", nullable = false, length = 40)
    private String estacaoCodigo;

    @Column(name = "estacao_nome", length = 160)
    private String estacaoNome;

    @Column(nullable = false, length = 160)
    private String municipio;

    @Column(nullable = false, length = 2)
    private String uf;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(name = "medido_em", nullable = false)
    private Instant medidoEm;

    @Column(name = "precipitacao_mm", nullable = false, precision = 8, scale = 2)
    private BigDecimal precipitacaoMm;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "importacao_id", nullable = false)
    private ImportacaoDataset importacao;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    protected RegistroMeteorologico() { }

    public RegistroMeteorologico(String estacaoCodigo, String estacaoNome, String municipio, String uf,
            double latitude, double longitude, Instant medidoEm, BigDecimal precipitacaoMm,
            ImportacaoDataset importacao) {
        this.estacaoCodigo = estacaoCodigo;
        this.estacaoNome = estacaoNome;
        this.municipio = municipio;
        this.uf = uf;
        this.latitude = latitude;
        this.longitude = longitude;
        this.medidoEm = medidoEm;
        this.precipitacaoMm = precipitacaoMm;
        this.importacao = importacao;
    }

    @PrePersist
    void aoCriar() { this.criadoEm = Instant.now(); }

    public Long getId() { return id; }
    public String getEstacaoCodigo() { return estacaoCodigo; }
    public String getEstacaoNome() { return estacaoNome; }
    public String getMunicipio() { return municipio; }
    public String getUf() { return uf; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public Instant getMedidoEm() { return medidoEm; }
    public BigDecimal getPrecipitacaoMm() { return precipitacaoMm; }
    public ImportacaoDataset getImportacao() { return importacao; }
    public Instant getCriadoEm() { return criadoEm; }
}
