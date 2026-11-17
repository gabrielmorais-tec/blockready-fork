package br.com.blockready.service;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import br.com.blockready.config.AppProperties;
import br.com.blockready.dto.ComponentStatus;
import br.com.blockready.dto.HealthResponse;

/**
 * Verifica a aplicação, a conexão com o PostgreSQL e a presença do PostGIS.
 */
@Service
public class HealthService {

    private static final Logger log = LoggerFactory.getLogger(HealthService.class);

    static final String SQL_DATABASE = "select 1";
    static final String SQL_POSTGIS = "select postgis_version()";

    private final JdbcTemplate jdbcTemplate;
    private final AppProperties properties;

    public HealthService(JdbcTemplate jdbcTemplate, AppProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
    }

    public HealthResponse check() {
        ComponentStatus database = checkDatabase();
        ComponentStatus postgis = database.isUp()
                ? checkPostgis()
                : ComponentStatus.down("não verificado: banco indisponível");
        String overall = database.isUp() && postgis.isUp() ? "UP" : "DOWN";

        return new HealthResponse(overall, "BLOCKREADY", properties.version(),
                properties.planningHorizonHours(), Instant.now(), database, postgis);
    }

    private ComponentStatus checkDatabase() {
        try {
            jdbcTemplate.queryForObject(SQL_DATABASE, Integer.class);
            return ComponentStatus.up(null);
        } catch (Exception ex) {
            log.warn("Health check: falha ao consultar o banco de dados", ex);
            return ComponentStatus.down("indisponível");
        }
    }

    private ComponentStatus checkPostgis() {
        try {
            String version = jdbcTemplate.queryForObject(SQL_POSTGIS, String.class);
            return ComponentStatus.up(version);
        } catch (Exception ex) {
            log.warn("Health check: PostGIS não respondeu", ex);
            return ComponentStatus.down("extensão PostGIS indisponível");
        }
    }
}
