package br.com.blockready.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.blockready.dto.Responses.AuditoriaResponse;
import br.com.blockready.dto.Responses.PageResponse;
import br.com.blockready.service.AuditoriaService;

@RestController
@RequestMapping("/api/v1/auditoria")
@PreAuthorize("hasRole('ADMIN')")
public class AuditoriaController {

    private final AuditoriaService service;

    public AuditoriaController(AuditoriaService service) { this.service = service; }

    @GetMapping
    public PageResponse<AuditoriaResponse> listar(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.listar(page, size);
    }
}
