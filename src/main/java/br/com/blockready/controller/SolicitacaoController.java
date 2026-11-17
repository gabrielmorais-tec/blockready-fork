package br.com.blockready.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.blockready.dto.Requests.AtualizarStatusRequest;
import br.com.blockready.dto.Requests.ResponsavelRequest;
import br.com.blockready.dto.Responses.HistoricoSolicitacaoResponse;
import br.com.blockready.dto.Responses.PageResponse;
import br.com.blockready.dto.Responses.SolicitacaoResponse;
import br.com.blockready.model.enums.CategoriaRecurso;
import br.com.blockready.model.enums.Prioridade;
import br.com.blockready.model.enums.StatusSolicitacao;
import br.com.blockready.security.UsuarioPrincipal;
import br.com.blockready.service.SolicitacaoService;
import br.com.blockready.service.SolicitacaoService.Filtro;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/solicitacoes")
public class SolicitacaoController {

    private final SolicitacaoService service;

    public SolicitacaoController(SolicitacaoService service) { this.service = service; }

    @GetMapping
    public PageResponse<SolicitacaoResponse> consultar(
            @RequestParam(required = false) StatusSolicitacao status,
            @RequestParam(required = false) CategoriaRecurso categoria,
            @RequestParam(required = false) Prioridade prioridade,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UsuarioPrincipal ator) {
        return service.buscar(new Filtro(status, categoria, prioridade), page, size, ator);
    }

    @GetMapping("/{id}")
    public SolicitacaoResponse obter(@PathVariable Long id, @AuthenticationPrincipal UsuarioPrincipal ator) {
        return service.obter(id, ator);
    }

    @PatchMapping("/{id}/status")
    public SolicitacaoResponse atualizarStatus(@PathVariable Long id,
            @Valid @RequestBody AtualizarStatusRequest request, @AuthenticationPrincipal UsuarioPrincipal ator) {
        return service.atualizarStatus(id, request, ator);
    }

    @PatchMapping("/{id}/responsavel")
    @PreAuthorize("hasAnyRole('GESTOR','ADMIN')")
    public SolicitacaoResponse atribuirResponsavel(@PathVariable Long id,
            @Valid @RequestBody ResponsavelRequest request, @AuthenticationPrincipal UsuarioPrincipal ator) {
        return service.atribuirResponsavel(id, request.usuarioId(), ator);
    }

    @GetMapping("/{id}/historico")
    public List<HistoricoSolicitacaoResponse> historico(@PathVariable Long id,
            @AuthenticationPrincipal UsuarioPrincipal ator) {
        return service.historico(id, ator);
    }
}
