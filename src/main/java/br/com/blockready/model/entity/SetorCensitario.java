package br.com.blockready.model.entity;

import java.math.BigDecimal;

import org.locationtech.jts.geom.MultiPolygon;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Setor censitário do IBGE (Censo 2022): população, domicílios e malha geográfica opcional. */
@Entity
@Table(name = "setor_censitario")
public class SetorCensitario extends EntidadeBase {

    @Column(name = "codigo_setor", nullable = false, length = 25, unique = true)
    private String codigoSetor;

    @Column(nullable = false, length = 160)
    private String municipio;

    @Column(nullable = false, length = 2)
    private String uf;

    @Column(nullable = false)
    private int populacao;

    @Column(nullable = false)
    private int domicilios;

    @Column(name = "area_km2", precision = 12, scale = 4)
    private BigDecimal areaKm2;

    /** Malha do setor em SRID 4326 (WGS 84), o mesmo sistema usado nos recursos comunitários. */
    @Column(columnDefinition = "geometry(MultiPolygon,4326)")
    private MultiPolygon geometria;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "importacao_id", nullable = false)
    private ImportacaoDataset importacao;

    protected SetorCensitario() { }

    public SetorCensitario(String codigoSetor, String municipio, String uf, int populacao, int domicilios,
            BigDecimal areaKm2, MultiPolygon geometria, ImportacaoDataset importacao) {
        this.codigoSetor = codigoSetor;
        this.municipio = municipio;
        this.uf = uf;
        this.populacao = populacao;
        this.domicilios = domicilios;
        this.areaKm2 = areaKm2;
        this.geometria = geometria;
        this.importacao = importacao;
    }

    /** Reimportação do mesmo setor: atualiza os valores preservando o identificador. */
    public void atualizar(String municipio, String uf, int populacao, int domicilios,
            BigDecimal areaKm2, MultiPolygon geometria, ImportacaoDataset importacao) {
        this.municipio = municipio;
        this.uf = uf;
        this.populacao = populacao;
        this.domicilios = domicilios;
        this.areaKm2 = areaKm2;
        if (geometria != null) {
            this.geometria = geometria;
        }
        this.importacao = importacao;
    }

    public String getCodigoSetor() { return codigoSetor; }
    public String getMunicipio() { return municipio; }
    public String getUf() { return uf; }
    public int getPopulacao() { return populacao; }
    public int getDomicilios() { return domicilios; }
    public BigDecimal getAreaKm2() { return areaKm2; }
    public MultiPolygon getGeometria() { return geometria; }
    public ImportacaoDataset getImportacao() { return importacao; }
}
