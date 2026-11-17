package br.com.blockready.model;

import jakarta.validation.constraints.Size;

/**
 * Atributos específicos dos subtipos de recurso. Cada subtipo usa apenas os campos que lhe pertencem.
 */
public record DetalhesRecurso(
        Boolean potavel,
        @Size(max = 80) String tipoFonte,
        Boolean aceitaAnimais,
        Boolean possuiRefeitorio,
        Boolean permiteCarregarDispositivos,
        Boolean refrigeracao,
        @Size(max = 80) String tipoAlimento) {
}
