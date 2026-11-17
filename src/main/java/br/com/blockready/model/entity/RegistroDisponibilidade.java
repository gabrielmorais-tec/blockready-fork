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
import jakarta.persistence.Table;

/** Registro imutável de capacidade e disponibilidade de um recurso em um instante. */
@Entity
@Table(name = "registro_disponibilidade")
public class RegistroDisponibilidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recurso_id", nullable = false)
    private RecursoComunitario recurso;

    @Column(name = "capacidade_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal capacidadeTotal;

    @Column(name = "capacidade_disponivel", nullable = false, precision = 12, scale = 2)
    private BigDecimal capacidadeDisponivel;

    @Column(length = 500)
    private String observacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registrado_por_id", nullable = false)
    private Usuario registradoPor;

    @Column(name = "registrado_em", nullable = false)
    private Instant registradoEm = Instant.now();

    protected RegistroDisponibilidade() { }

    public RegistroDisponibilidade(RecursoComunitario recurso, BigDecimal total, BigDecimal disponivel,
            String observacao, Usuario registradoPor) {
        this.recurso = recurso;
        this.capacidadeTotal = total;
        this.capacidadeDisponivel = disponivel;
        this.observacao = observacao;
        this.registradoPor = registradoPor;
    }

    public Long getId() { return id; }
    public RecursoComunitario getRecurso() { return recurso; }
    public BigDecimal getCapacidadeTotal() { return capacidadeTotal; }
    public BigDecimal getCapacidadeDisponivel() { return capacidadeDisponivel; }
    public String getObservacao() { return observacao; }
    public Usuario getRegistradoPor() { return registradoPor; }
    public Instant getRegistradoEm() { return registradoEm; }
}
