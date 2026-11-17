package br.com.blockready.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.blockready.dto.Requests.NecessidadeRequest;
import br.com.blockready.dto.Responses.SolicitacaoResponse;
import br.com.blockready.security.UsuarioPrincipal;
import br.com.blockready.service.NecessidadeService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/necessidades")
public class NecessidadeController {

    private final NecessidadeService service;

    public NecessidadeController(NecessidadeService service) { this.service = service; }

    /** Registra a necessidade e abre a solicitação correspondente. Para acompanhar, use /api/v1/solicitacoes. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('MORADOR')")
    public SolicitacaoResponse registrar(@Valid @RequestBody NecessidadeRequest request,
            @AuthenticationPrincipal UsuarioPrincipal ator) {
        return service.registrar(request, ator);
    }
}
