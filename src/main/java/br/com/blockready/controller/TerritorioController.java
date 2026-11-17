package br.com.blockready.controller;

import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.blockready.dto.Responses.PageResponse;
import br.com.blockready.dto.Responses.SetorCensitarioResponse;
import br.com.blockready.dto.Responses.TerritorioResumoResponse;
import br.com.blockready.service.TerritorioService;

/**
 * Dados territoriais importados. O resumo e o GeoJSON ficam disponíveis a qualquer usuário
 * autenticado (alimentam o mapa); a listagem detalhada é restrita a gestores/administradores.
 */
@RestController
@RequestMapping("/api/v1/territorios")
public class TerritorioController {

    private final TerritorioService service;

    public TerritorioController(TerritorioService service) { this.service = service; }

    @GetMapping("/resumo")
    public TerritorioResumoResponse resumo() {
        return service.resumo();
    }

    @GetMapping("/setores")
    @PreAuthorize("hasAnyRole('GESTOR','ADMIN')")
    public PageResponse<SetorCensitarioResponse> setores(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.listarSetores(page, size);
    }

    @GetMapping("/setores/geojson")
    public Map<String, Object> setoresGeoJson() {
        return service.setoresGeoJson();
    }

    @GetMapping("/estacoes")
    public List<Map<String, Object>> estacoes(@RequestParam(defaultValue = "72") int horas) {
        return service.estacoes(horas);
    }
}
