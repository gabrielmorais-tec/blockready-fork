package br.com.blockready.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.blockready.dto.Requests.DisponibilidadeRequest;
import br.com.blockready.dto.Requests.MotivoRequest;
import br.com.blockready.dto.Requests.RecursoRequest;
import br.com.blockready.dto.Responses.DisponibilidadeResponse;
import br.com.blockready.dto.Responses.HistoricoRecursoResponse;
import br.com.blockready.dto.Responses.PageResponse;
import br.com.blockready.dto.Responses.RecursoResponse;
import br.com.blockready.model.enums.CategoriaRecurso;
import br.com.blockready.model.enums.StatusRecurso;
import br.com.blockready.security.UsuarioPrincipal;
import br.com.blockready.service.RecursoService;
import br.com.blockready.service.RecursoService.Filtro;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/recursos")
public class RecursoController {

    private final RecursoService service;

    public RecursoController(RecursoService service) { this.service = service; }

    @GetMapping
    public PageResponse<RecursoResponse> consultar(
            @RequestParam(required = false) CategoriaRecurso categoria,
            @RequestParam(required = false) StatusRecurso status,
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lon,
            @RequestParam(required = false) Double raioKm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UsuarioPrincipal ator) {
        return service.buscar(new Filtro(categoria, status, busca, lat, lon, raioKm), page, size, ator);
    }

    @GetMapping("/{id}")
    public RecursoResponse obter(@PathVariable Long id, @AuthenticationPrincipal UsuarioPrincipal ator) {
        return service.obter(id, ator);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ORGANIZACAO','GESTOR','ADMIN')")
    public RecursoResponse criar(@Valid @RequestBody RecursoRequest request,
            @AuthenticationPrincipal UsuarioPrincipal ator) {
        return service.criar(request, ator);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORGANIZACAO','GESTOR','ADMIN')")
    public RecursoResponse atualizar(@PathVariable Long id, @Valid @RequestBody RecursoRequest request,
            @AuthenticationPrincipal UsuarioPrincipal ator) {
        return service.atualizar(id, request, ator);
    }

    @PatchMapping("/{id}/disponibilidade")
    @PreAuthorize("hasAnyRole('ORGANIZACAO','GESTOR','ADMIN')")
    public RecursoResponse atualizarDisponibilidade(@PathVariable Long id,
            @Valid @RequestBody DisponibilidadeRequest request, @AuthenticationPrincipal UsuarioPrincipal ator) {
        return service.atualizarDisponibilidade(id, request, ator);
    }

    @PostMapping("/{id}/validar")
    @PreAuthorize("hasAnyRole('GESTOR','ADMIN')")
    public RecursoResponse validar(@PathVariable Long id, @AuthenticationPrincipal UsuarioPrincipal ator) {
        return service.validar(id, ator);
    }

    @PostMapping("/{id}/rejeitar")
    @PreAuthorize("hasAnyRole('GESTOR','ADMIN')")
    public RecursoResponse rejeitar(@PathVariable Long id, @Valid @RequestBody(required = false) MotivoRequest request,
            @AuthenticationPrincipal UsuarioPrincipal ator) {
        return service.rejeitar(id, request != null ? request.motivo() : null, ator);
    }

    /** Desativa o recurso (não apaga registros), preservando histórico e disponibilidade. */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORGANIZACAO','GESTOR','ADMIN')")
    public RecursoResponse desativar(@PathVariable Long id, @AuthenticationPrincipal UsuarioPrincipal ator) {
        return service.desativar(id, ator);
    }

    @GetMapping("/{id}/historico")
    @PreAuthorize("hasAnyRole('ORGANIZACAO','GESTOR','ADMIN')")
    public List<HistoricoRecursoResponse> historico(@PathVariable Long id,
            @AuthenticationPrincipal UsuarioPrincipal ator) {
        return service.historico(id, ator);
    }

    @GetMapping("/{id}/disponibilidades")
    @PreAuthorize("hasAnyRole('ORGANIZACAO','GESTOR','ADMIN')")
    public PageResponse<DisponibilidadeResponse> disponibilidades(@PathVariable Long id,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UsuarioPrincipal ator) {
        return service.disponibilidades(id, page, size, ator);
    }
}
