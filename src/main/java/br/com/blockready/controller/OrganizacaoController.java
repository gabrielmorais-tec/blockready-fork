package br.com.blockready.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.blockready.dto.Requests.OrganizacaoRequest;
import br.com.blockready.dto.Responses.OrganizacaoResponse;
import br.com.blockready.dto.Responses.PageResponse;
import br.com.blockready.model.enums.StatusAprovacao;
import br.com.blockready.security.UsuarioPrincipal;
import br.com.blockready.service.OrganizacaoService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/organizacoes")
public class OrganizacaoController {

    private final OrganizacaoService service;

    public OrganizacaoController(OrganizacaoService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("hasAnyRole('GESTOR','ADMIN')")
    public PageResponse<OrganizacaoResponse> listar(@RequestParam(required = false) StatusAprovacao status,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return service.listar(status, page, size);
    }

    @GetMapping("/minha")
    @PreAuthorize("hasRole('ORGANIZACAO')")
    public OrganizacaoResponse minha(@AuthenticationPrincipal UsuarioPrincipal ator) {
        return service.minha(ator);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORGANIZACAO','GESTOR','ADMIN')")
    public OrganizacaoResponse obter(@PathVariable Long id, @AuthenticationPrincipal UsuarioPrincipal ator) {
        return service.obter(id, ator);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORGANIZACAO','GESTOR','ADMIN')")
    public OrganizacaoResponse atualizar(@PathVariable Long id, @Valid @RequestBody OrganizacaoRequest request,
            @AuthenticationPrincipal UsuarioPrincipal ator) {
        return service.atualizar(id, request, ator);
    }

    @PostMapping("/{id}/aprovar")
    @PreAuthorize("hasAnyRole('GESTOR','ADMIN')")
    public OrganizacaoResponse aprovar(@PathVariable Long id, @AuthenticationPrincipal UsuarioPrincipal ator) {
        return service.decidir(id, StatusAprovacao.APROVADA, ator);
    }

    @PostMapping("/{id}/rejeitar")
    @PreAuthorize("hasAnyRole('GESTOR','ADMIN')")
    public OrganizacaoResponse rejeitar(@PathVariable Long id, @AuthenticationPrincipal UsuarioPrincipal ator) {
        return service.decidir(id, StatusAprovacao.REJEITADA, ator);
    }

    @PostMapping("/{id}/suspender")
    @PreAuthorize("hasAnyRole('GESTOR','ADMIN')")
    public OrganizacaoResponse suspender(@PathVariable Long id, @AuthenticationPrincipal UsuarioPrincipal ator) {
        return service.decidir(id, StatusAprovacao.SUSPENSA, ator);
    }
}
