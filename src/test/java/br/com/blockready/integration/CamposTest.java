package br.com.blockready.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class CamposTest {

    @Test
    void converteNumerosPtBrEInternacionais() {
        assertEquals(new BigDecimal("1234.56"), Campos.decimal("1.234,56"));
        assertEquals(new BigDecimal("1234.56"), Campos.decimal("1234.56"));
        assertEquals(Integer.valueOf(1500), Campos.inteiro("1.500"));
        assertNull(Campos.decimal("abc"));
        assertNull(Campos.inteiro(""));
    }

    @Test
    void converteDatasEmVariosFormatos() {
        assertEquals(LocalDate.of(2024, 5, 3), Campos.data("2024-05-03"));
        assertEquals(LocalDate.of(2024, 5, 3), Campos.data("03/05/2024"));
        assertNull(Campos.data("data inválida"));
    }

    @Test
    void converteDataHoraParaUtc() {
        assertEquals(Instant.parse("2024-05-03T12:30:00Z"), Campos.instanteUtc("2024-05-03 12:30:00"));
        assertEquals(Instant.parse("2024-05-03T12:30:00Z"), Campos.instanteUtc("2024-05-03T12:30:00Z"));
        assertNull(Campos.instanteUtc("ontem"));
    }

    @Test
    void coordenadaForaDoIntervaloEhNula() {
        assertEquals(-25.43, Campos.coordenada("-25,43", -90, 90));
        assertNull(Campos.coordenada("95", -90, 90));
    }
}
