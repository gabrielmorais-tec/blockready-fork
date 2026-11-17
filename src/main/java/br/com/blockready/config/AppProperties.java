package br.com.blockready.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriedades da aplicação, lidas do prefixo {@code blockready} em application.yml.
 *
 * @param version              versão da aplicação (vem do pom.xml)
 * @param planningHorizonHours horizonte de planejamento em horas (referência de planejamento, não uma garantia)
 */
@ConfigurationProperties(prefix = "blockready")
public record AppProperties(String version, int planningHorizonHours) {
}
