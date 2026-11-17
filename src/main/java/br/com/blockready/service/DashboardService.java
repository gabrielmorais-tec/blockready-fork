package br.com.blockready.service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.blockready.dto.Responses.DashboardResponse;
import br.com.blockready.model.enums.CategoriaRecurso;
import br.com.blockready.model.enums.StatusAprovacao;
import br.com.blockready.model.enums.StatusRecurso;
import br.com.blockready.model.enums.StatusSolicitacao;
import br.com.blockready.repository.OrganizacaoRepository;
import br.com.blockready.repository.RecursoRepository;
import br.com.blockready.repository.SolicitacaoRepository;
import br.com.blockready.repository.VoluntarioRepository;

/** Indicadores agregados lidos do banco. Contém apenas contagens, nunca dados pessoais. */
@Service
public class DashboardService {

    private final RecursoRepository recursos;
    private final SolicitacaoRepository solicitacoes;
    private final VoluntarioRepository voluntarios;
    private final OrganizacaoRepository organizacoes;

    public DashboardService(RecursoRepository recursos, SolicitacaoRepository solicitacoes,
            VoluntarioRepository voluntarios, OrganizacaoRepository organizacoes) {
        this.recursos = recursos;
        this.solicitacoes = solicitacoes;
        this.voluntarios = voluntarios;
        this.organizacoes = organizacoes;
    }

    @Transactional(readOnly = true)
    public DashboardResponse gerar() {
        Map<String, Long> porCategoria = new LinkedHashMap<>();
        for (CategoriaRecurso c : CategoriaRecurso.values()) {
            porCategoria.put(c.name(), 0L);
        }
        for (Object[] linha : recursos.contarPorCategoria(StatusRecurso.VALIDADO)) {
            porCategoria.put(((CategoriaRecurso) linha[0]).name(), (Long) linha[1]);
        }
        Map<String, Long> porStatus = new LinkedHashMap<>();
        for (StatusSolicitacao s : StatusSolicitacao.values()) {
            porStatus.put(s.name(), 0L);
        }
        for (Object[] linha : solicitacoes.contarPorStatus()) {
            porStatus.put(((StatusSolicitacao) linha[0]).name(), (Long) linha[1]);
        }
        long emAberto = porStatus.entrySet().stream()
                .filter(e -> !StatusSolicitacao.valueOf(e.getKey()).isFinal()).mapToLong(Map.Entry::getValue).sum();

        return new DashboardResponse(Instant.now(),
                recursos.countByStatus(StatusRecurso.VALIDADO),
                recursos.countByStatus(StatusRecurso.PENDENTE_VALIDACAO),
                emAberto, porStatus.get(StatusSolicitacao.ATENDIDA.name()),
                voluntarios.countByAtivoTrue(),
                organizacoes.countByStatusAprovacao(StatusAprovacao.PENDENTE),
                porCategoria, porStatus);
    }
}
