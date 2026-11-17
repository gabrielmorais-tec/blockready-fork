package br.com.blockready.dto;

import java.time.Instant;

/**
 * Resposta do endpoint {@code GET /api/v1/health}.
 */
public record HealthResponse(
        String status,
        String application,
        String version,
        int planningHorizonHours,
        Instant timestamp,
        ComponentStatus database,
        ComponentStatus postgis) {
}
