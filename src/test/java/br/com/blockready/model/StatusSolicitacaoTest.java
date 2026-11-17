package br.com.blockready.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import br.com.blockready.model.enums.StatusSolicitacao;

class StatusSolicitacaoTest {

    @Test
    void fluxoNormalAvancaUmPassoPorVez() {
        assertThat(StatusSolicitacao.ABERTA.podeIrPara(StatusSolicitacao.EM_ANALISE)).isTrue();
        assertThat(StatusSolicitacao.EM_ANALISE.podeIrPara(StatusSolicitacao.EM_ATENDIMENTO)).isTrue();
        assertThat(StatusSolicitacao.EM_ATENDIMENTO.podeIrPara(StatusSolicitacao.ATENDIDA)).isTrue();
    }

    @Test
    void naoPermitePularEtapasNemVoltar() {
        assertThat(StatusSolicitacao.ABERTA.podeIrPara(StatusSolicitacao.ATENDIDA)).isFalse();
        assertThat(StatusSolicitacao.EM_ATENDIMENTO.podeIrPara(StatusSolicitacao.ABERTA)).isFalse();
    }

    @Test
    void statusFinaisNaoTemProximo() {
        assertThat(StatusSolicitacao.ATENDIDA.isFinal()).isTrue();
        assertThat(StatusSolicitacao.CANCELADA.isFinal()).isTrue();
        assertThat(StatusSolicitacao.ABERTA.isFinal()).isFalse();
    }

    @Test
    void cancelamentoEPossivelAteOAtendimento() {
        assertThat(StatusSolicitacao.ABERTA.podeIrPara(StatusSolicitacao.CANCELADA)).isTrue();
        assertThat(StatusSolicitacao.EM_ATENDIMENTO.podeIrPara(StatusSolicitacao.CANCELADA)).isTrue();
    }
}
