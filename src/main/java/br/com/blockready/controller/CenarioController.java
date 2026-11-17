package br.com.blockready.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.blockready.dto.Analytics.CenarioRequest;
import br.com.blockready.dto.Analytics.CenarioResponse;
import br.com.blockready.dto.Analytics.CenarioResumoResponse;
import br.com.blockready.dto.Responses.PageResponse;
import br.com.blockready.security.UsuarioPrincipal;
import br.com.blockready.service.CenarioService;
import jakarta.validation.Valid;

/** Ferramenta de simulação de cenários de emergência (planejamento 72 h), restrita a staff. */
@RestController
@RequestMapping("/api/v1/cenarios")
@PreAuthorize("hasAnyRole('GESTOR','ADMIN')")
public class CenarioController {

    private final CenarioService service;

    public CenarioController(CenarioService service) { this.service = service; }

    /** Simula sem salvar: usado para explorar parâmetros antes de guardar o cenário. */
    @PostMapping("/simular")
    public CenarioResponse simular(@Valid @RequestBody CenarioRequest request) {
        return service.simular(request);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CenarioResponse salvar(@Valid @RequestBody CenarioRequest request,
            @AuthenticationPrincipal UsuarioPrincipal ator) {
        return service.salvar(request, ator);
    }

    @GetMapping
    public PageResponse<CenarioResumoResponse> listar(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.listar(page, size);
    }

    @GetMapping("/{id}")
    public CenarioResponse obter(@PathVariable Long id) {
        return service.obter(id);
    }
}
