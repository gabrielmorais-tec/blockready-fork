package br.com.blockready.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Estado de um componente verificado pelo health check.
 *
 * @param status {@code UP} ou {@code DOWN}
 * @param detail informação complementar (por exemplo, a versão do PostGIS)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ComponentStatus(String status, String detail) {

    public static ComponentStatus up(String detail) {
        return new ComponentStatus("UP", detail);
    }

    public static ComponentStatus down(String detail) {
        return new ComponentStatus("DOWN", detail);
    }

    @JsonIgnore
    public boolean isUp() {
        return "UP".equals(status);
    }
}
