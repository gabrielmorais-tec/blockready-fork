package br.com.blockready.mapper;

import br.com.blockready.dto.Responses.DisponibilidadeResponse;
import br.com.blockready.dto.Responses.HistoricoRecursoResponse;
import br.com.blockready.dto.Responses.RecursoResponse;
import br.com.blockready.model.entity.RecursoComunitario;
import br.com.blockready.model.entity.RecursoHistorico;
import br.com.blockready.model.entity.RegistroDisponibilidade;
import br.com.blockready.model.entity.Usuario;

public final class RecursoMapper {

    private RecursoMapper() { }

    /**
     * @param verResponsavel quando falso, omite quem validou o recurso (dado de pessoa).
     */
    public static RecursoResponse toResponse(RecursoComunitario r, boolean verResponsavel) {
        Usuario validador = verResponsavel ? r.getValidadoPor() : null;
        return new RecursoResponse(r.getId(), r.getNome(), r.getCategoria(), r.getDescricao(), r.getEndereco(),
                r.getLatitude(), r.getLongitude(), r.getCapacidadeTotal(), r.getCapacidadeDisponivel(),
                r.getUnidadeMedida(), r.descreverCapacidade(), r.getHorarioFuncionamento(), r.isAcessivel(),
                r.getAcessibilidadeDescricao(), r.getStatus(), r.estaConfirmado(), r.getAtualizadoEm(),
                r.getOrganizacao().getId(), r.getOrganizacao().getNome(),
                validador != null ? validador.getId() : null, validador != null ? validador.getNome() : null,
                verResponsavel ? r.getValidadoEm() : null, r.detalhesEspecificos());
    }

    public static DisponibilidadeResponse toResponse(RegistroDisponibilidade d) {
        return new DisponibilidadeResponse(d.getId(), d.getCapacidadeTotal(), d.getCapacidadeDisponivel(),
                d.getObservacao(), d.getRegistradoPor().getNome(), d.getRegistradoEm());
    }

    public static HistoricoRecursoResponse toResponse(RecursoHistorico h) {
        return new HistoricoRecursoResponse(h.getId(), h.getAcao(), h.getDescricao(), h.getUsuario().getNome(),
                h.getCriadoEm());
    }
}
