package br.com.blockready.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.blockready.dto.Requests.VoluntarioRequest;
import br.com.blockready.dto.Responses.PageResponse;
import br.com.blockready.dto.Responses.VoluntarioResponse;
import br.com.blockready.security.UsuarioPrincipal;
import br.com.blockready.service.VoluntarioService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/voluntarios")
public class VoluntarioController {

    private final VoluntarioService service;

    public VoluntarioController(VoluntarioService service) { this.service = service; }

    @GetMapping("/me")
    public VoluntarioResponse meu(@AuthenticationPrincipal UsuarioPrincipal ator) {
        return service.meu(ator);
    }

    @PutMapping("/me")
    public VoluntarioResponse salvar(@Valid @RequestBody VoluntarioRequest request,
            @AuthenticationPrincipal UsuarioPrincipal ator) {
        return service.salvar(request, ator);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('GESTOR','ADMIN')")
    public PageResponse<VoluntarioResponse> listar(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.listar(page, size);
    }
}
