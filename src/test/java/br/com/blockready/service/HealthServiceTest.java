package br.com.blockready.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

import br.com.blockready.config.AppProperties;
import br.com.blockready.dto.HealthResponse;

class HealthServiceTest {

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final HealthService service = new HealthService(jdbc, new AppProperties("0.1.0", 72));

    @Test
    void reportsUpWhenDatabaseAndPostgisRespond() {
        when(jdbc.queryForObject(HealthService.SQL_DATABASE, Integer.class)).thenReturn(1);
        when(jdbc.queryForObject(HealthService.SQL_POSTGIS, String.class)).thenReturn("3.4 USE_GEOS=1");

        HealthResponse response = service.check();

        assertThat(response.status()).isEqualTo("UP");
        assertThat(response.postgis().detail()).startsWith("3.4");
        assertThat(response.planningHorizonHours()).isEqualTo(72);
    }

    @Test
    void reportsDownWhenDatabaseFails() {
        when(jdbc.queryForObject(HealthService.SQL_DATABASE, Integer.class))
                .thenThrow(new DataAccessResourceFailureException("sem conexão"));

        HealthResponse response = service.check();

        assertThat(response.status()).isEqualTo("DOWN");
        assertThat(response.database().status()).isEqualTo("DOWN");
        assertThat(response.postgis().status()).isEqualTo("DOWN");
    }

    @Test
    void reportsDownWhenPostgisIsMissing() {
        when(jdbc.queryForObject(HealthService.SQL_DATABASE, Integer.class)).thenReturn(1);
        when(jdbc.queryForObject(HealthService.SQL_POSTGIS, String.class))
                .thenThrow(new DataAccessResourceFailureException("função inexistente"));

        HealthResponse response = service.check();

        assertThat(response.status()).isEqualTo("DOWN");
        assertThat(response.database().status()).isEqualTo("UP");
        assertThat(response.postgis().status()).isEqualTo("DOWN");
    }
}
