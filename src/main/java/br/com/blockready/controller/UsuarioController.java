package br.com.blockready.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.blockready.dto.Requests.AtivoRequest;
import br.com.blockready.dto.Requests.CriarUsuarioRequest;
import br.com.blockready.dto.Requests.PerfilRequest;
import br.com.blockready.dto.Responses.PageResponse;
import br.com.blockready.dto.Responses.UsuarioResponse;
import br.com.blockready.security.UsuarioPrincipal;
import br.com.blockready.service.UsuarioService;
import jakarta.validation.Valid;

/** Gestão de usuários e permissões: somente administradores. */
@RestController
@RequestMapping("/api/v1/usuarios")
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) { this.service = service; }

    @GetMapping
    public PageResponse<UsuarioResponse> listar(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.listar(page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse criar(@Valid @RequestBody CriarUsuarioRequest request,
            @AuthenticationPrincipal UsuarioPrincipal ator) {
        return service.criar(request, ator);
    }

    @PatchMapping("/{id}/perfil")
    public UsuarioResponse alterarPerfil(@PathVariable Long id, @Valid @RequestBody PerfilRequest request,
            @AuthenticationPrincipal UsuarioPrincipal ator) {
        return service.alterarPerfil(id, request.perfil(), ator);
    }

    @PatchMapping("/{id}/ativo")
    public UsuarioResponse alterarAtivo(@PathVariable Long id, @Valid @RequestBody AtivoRequest request,
            @AuthenticationPrincipal UsuarioPrincipal ator) {
        return service.alterarAtivo(id, request.ativo(), ator);
    }
}
