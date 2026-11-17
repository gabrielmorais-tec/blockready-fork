package br.com.blockready.mapper;

import java.util.List;

import br.com.blockready.dto.Responses.HistoricoSolicitacaoResponse;
import br.com.blockready.dto.Responses.SolicitacaoResponse;
import br.com.blockready.model.entity.Necessidade;
import br.com.blockready.model.entity.Solicitacao;
import br.com.blockready.model.entity.SolicitacaoHistorico;
import br.com.blockready.model.entity.Usuario;

public final class SolicitacaoMapper {

    private SolicitacaoMapper() { }

    /**
     * @param detalhesPessoais quando falso, omite endereço, coordenadas e nomes de pessoas.
     */
    public static SolicitacaoResponse toResponse(Solicitacao s, boolean detalhesPessoais) {
        Necessidade n = s.getNecessidade();
        Usuario resp = s.getResponsavel();
        return new SolicitacaoResponse(s.getId(), n.getId(), n.getCategoria(), n.getDescricao(), n.getQuantidade(),
                n.getUnidade(), s.getPrioridade(), s.getStatus(), s.getCriadoEm(), s.getAtualizadoEm(),
                detalhesPessoais ? s.getRegistroAtendimento() : null, s.getAtendidaEm(),
                detalhesPessoais ? n.getEndereco() : null,
                detalhesPessoais ? n.getLatitude() : null,
                detalhesPessoais ? n.getLongitude() : null,
                detalhesPessoais ? n.getMorador().getNome() : null,
                detalhesPessoais && resp != null ? resp.getId() : null,
                detalhesPessoais && resp != null ? resp.getNome() : null,
                List.copyOf(s.getStatus().proximos()));
    }

    public static HistoricoSolicitacaoResponse toResponse(SolicitacaoHistorico h, boolean mostrarNomes) {
        return new HistoricoSolicitacaoResponse(h.getId(), h.getStatusAnterior(), h.getStatusNovo(),
                h.getObservacao(), mostrarNomes ? h.getUsuario().getNome() : null, h.getCriadoEm());
    }
}
