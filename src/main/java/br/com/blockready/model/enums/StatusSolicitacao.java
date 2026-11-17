package br.com.blockready.model.enums;

import java.util.EnumSet;
import java.util.Set;

/** Fluxo: ABERTA -> EM_ANALISE -> EM_ATENDIMENTO -> ATENDIDA; CANCELADA é possível até o atendimento. */
public enum StatusSolicitacao {
    ABERTA, EM_ANALISE, EM_ATENDIMENTO, ATENDIDA, CANCELADA;

    public Set<StatusSolicitacao> proximos() {
        return switch (this) {
            case ABERTA -> EnumSet.of(EM_ANALISE, CANCELADA);
            case EM_ANALISE -> EnumSet.of(EM_ATENDIMENTO, CANCELADA);
            case EM_ATENDIMENTO -> EnumSet.of(ATENDIDA, CANCELADA);
            case ATENDIDA, CANCELADA -> EnumSet.noneOf(StatusSolicitacao.class);
        };
    }

    public boolean podeIrPara(StatusSolicitacao destino) { return proximos().contains(destino); }

    public boolean isFinal() { return proximos().isEmpty(); }
}
