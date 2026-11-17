package br.com.blockready.model.entity;

import java.math.BigDecimal;
import java.time.Instant;

import br.com.blockready.model.enums.CategoriaRecurso;
import br.com.blockready.model.enums.Prioridade;
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
import jakarta.persistence.Table;

/** Necessidade registrada por um morador. Dados pessoais (endereço, autor) só são expostos a quem pode atendê-la. */
@Entity
@Table(name = "necessidade")
public class Necessidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "morador_id", nullable = false)
    private Usuario morador;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private CategoriaRecurso categoria;

    @Column(nullable = false, columnDefinition = "text")
    private String descricao;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal quantidade;

    @Column(nullable = false, length = 30)
    private String unidade;

    @Column(nullable = false, length = 300)
    private String endereco;

    private Double latitude;
    private Double longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Prioridade prioridade;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm = Instant.now();

    protected Necessidade() { }

    public Necessidade(Usuario morador, CategoriaRecurso categoria, String descricao, BigDecimal quantidade,
            String unidade, String endereco, Double latitude, Double longitude, Prioridade prioridade) {
        this.morador = morador;
        this.categoria = categoria;
        this.descricao = descricao;
        this.quantidade = quantidade;
        this.unidade = unidade;
        this.endereco = endereco;
        this.latitude = latitude;
        this.longitude = longitude;
        this.prioridade = prioridade;
    }

    public Long getId() { return id; }
    public Usuario getMorador() { return morador; }
    public CategoriaRecurso getCategoria() { return categoria; }
    public String getDescricao() { return descricao; }
    public BigDecimal getQuantidade() { return quantidade; }
    public String getUnidade() { return unidade; }
    public String getEndereco() { return endereco; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public Prioridade getPrioridade() { return prioridade; }
    public Instant getCriadoEm() { return criadoEm; }
}
