package br.com.blockready.model.entity;

import java.time.Instant;

import br.com.blockready.model.enums.StatusSolicitacao;
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

/** Cada mudança de status de uma solicitação. */
@Entity
@Table(name = "solicitacao_historico")
public class SolicitacaoHistorico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitacao_id", nullable = false)
    private Solicitacao solicitacao;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_anterior", length = 20)
    private StatusSolicitacao statusAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_novo", nullable = false, length = 20)
    private StatusSolicitacao statusNovo;

    @Column(length = 1000)
    private String observacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm = Instant.now();

    protected SolicitacaoHistorico() { }

    public SolicitacaoHistorico(Solicitacao solicitacao, StatusSolicitacao anterior, StatusSolicitacao novo,
            String observacao, Usuario usuario) {
        this.solicitacao = solicitacao;
        this.statusAnterior = anterior;
        this.statusNovo = novo;
        this.observacao = observacao;
        this.usuario = usuario;
    }

    public Long getId() { return id; }
    public StatusSolicitacao getStatusAnterior() { return statusAnterior; }
    public StatusSolicitacao getStatusNovo() { return statusNovo; }
    public String getObservacao() { return observacao; }
    public Usuario getUsuario() { return usuario; }
    public Instant getCriadoEm() { return criadoEm; }
}
