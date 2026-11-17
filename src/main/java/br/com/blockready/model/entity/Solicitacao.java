package br.com.blockready.model.entity;

import java.time.Instant;

import br.com.blockready.exception.BusinessException;
import br.com.blockready.model.enums.Prioridade;
import br.com.blockready.model.enums.StatusSolicitacao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/** Acompanhamento de uma necessidade, com fluxo de status controlado por {@link StatusSolicitacao}. */
@Entity
@Table(name = "solicitacao")
public class Solicitacao extends EntidadeBase {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "necessidade_id", nullable = false, unique = true)
    private Necessidade necessidade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusSolicitacao status = StatusSolicitacao.ABERTA;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Prioridade prioridade;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsavel_id")
    private Usuario responsavel;

    @Column(name = "registro_atendimento", columnDefinition = "text")
    private String registroAtendimento;

    @Column(name = "atendida_em")
    private Instant atendidaEm;

    protected Solicitacao() { }

    public Solicitacao(Necessidade necessidade) {
        this.necessidade = necessidade;
        this.prioridade = necessidade.getPrioridade();
    }

    /** Aplica a transição validando o fluxo. O responsável é quem assumiu o acompanhamento. */
    public void transitarPara(StatusSolicitacao novo, Usuario ator, String registroAtendimento) {
        if (!status.podeIrPara(novo)) {
            throw new BusinessException("Transição de status inválida: " + status + " -> " + novo + ".");
        }
        if (novo == StatusSolicitacao.ATENDIDA) {
            if (registroAtendimento == null || registroAtendimento.isBlank()) {
                throw new BusinessException("Informe o registro do atendimento realizado.");
            }
            this.registroAtendimento = registroAtendimento.trim();
            this.atendidaEm = Instant.now();
        }
        if (this.responsavel == null && novo != StatusSolicitacao.CANCELADA && ator != null) {
            this.responsavel = ator;
        }
        this.status = novo;
    }

    public void atribuirResponsavel(Usuario novo) { this.responsavel = novo; }

    public Necessidade getNecessidade() { return necessidade; }
    public StatusSolicitacao getStatus() { return status; }
    public Prioridade getPrioridade() { return prioridade; }
    public Usuario getResponsavel() { return responsavel; }
    public String getRegistroAtendimento() { return registroAtendimento; }
    public Instant getAtendidaEm() { return atendidaEm; }
}
