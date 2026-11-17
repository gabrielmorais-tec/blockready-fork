package br.com.blockready.model.entity;

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

/** Histórico de alterações de um recurso. */
@Entity
@Table(name = "recurso_historico")
public class RecursoHistorico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recurso_id", nullable = false)
    private RecursoComunitario recurso;

    @Column(nullable = false, length = 40)
    private String acao;

    @Column(length = 500)
    private String descricao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm = Instant.now();

    protected RecursoHistorico() { }

    public RecursoHistorico(RecursoComunitario recurso, String acao, String descricao, Usuario usuario) {
        this.recurso = recurso;
        this.acao = acao;
        this.descricao = descricao;
        this.usuario = usuario;
    }

    public Long getId() { return id; }
    public String getAcao() { return acao; }
    public String getDescricao() { return descricao; }
    public Usuario getUsuario() { return usuario; }
    public Instant getCriadoEm() { return criadoEm; }
}
