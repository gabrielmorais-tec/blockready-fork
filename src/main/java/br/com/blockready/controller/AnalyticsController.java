package br.com.blockready.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.blockready.analytics.CriService;
import br.com.blockready.analytics.InsightService;
import br.com.blockready.dto.Analytics.CriResponse;
import br.com.blockready.dto.Analytics.InsightResponse;

/**
 * Inteligência analítica: Community Resilience Index e os quatro insights estratégicos.
 * O CRI agregado abastece o painel de qualquer usuário autenticado; os insights são de staff.
 */
@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final CriService criService;
    private final InsightService insights;

    public AnalyticsController(CriService criService, InsightService insights) {
        this.criService = criService;
        this.insights = insights;
    }

    @GetMapping("/cri")
    public CriResponse cri() {
        return criService.calcularPadrao();
    }

    @GetMapping("/insights")
    @PreAuthorize("hasAnyRole('GESTOR','ADMIN')")
    public List<InsightResponse> insights() {
        return insights.todos();
    }

    @GetMapping("/insights/{codigo}")
    @PreAuthorize("hasAnyRole('GESTOR','ADMIN')")
    public InsightResponse insight(@PathVariable String codigo) {
        return insights.porCodigo(codigo);
    }
}
