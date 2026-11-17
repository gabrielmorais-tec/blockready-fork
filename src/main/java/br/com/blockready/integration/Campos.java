package br.com.blockready.integration;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/** Conversões tolerantes dos campos dos datasets públicos (números pt-BR, datas e horários UTC). */
public final class Campos {

    private static final DateTimeFormatter[] DATAS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("yyyyMMdd")
    };

    private static final DateTimeFormatter[] DATAS_HORA = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
    };

    private Campos() { }

    public static Integer inteiro(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            String limpo = valor.trim().replace(".", "").replace(",", ".");
            BigDecimal n = new BigDecimal(limpo);
            return n.intValueExact();
        } catch (ArithmeticException | NumberFormatException e) {
            return null;
        }
    }

    /** Número decimal aceitando formato pt-BR (1.234,56) e internacional (1234.56). */
    public static BigDecimal decimal(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String limpo = valor.trim().replace(" ", "");
        if (limpo.contains(",")) {
            limpo = limpo.replace(".", "").replace(",", ".");
        }
        try {
            return new BigDecimal(limpo);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Double coordenada(String valor, double min, double max) {
        BigDecimal n = decimal(valor);
        if (n == null) {
            return null;
        }
        double d = n.doubleValue();
        return d >= min && d <= max ? d : null;
    }

    public static LocalDate data(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String limpo = valor.trim();
        for (DateTimeFormatter f : DATAS) {
            try {
                return LocalDate.parse(limpo, f);
            } catch (Exception e) { /* tenta o próximo formato */ }
        }
        try {
            return LocalDateTime.parse(limpo.replace(' ', 'T')).toLocalDate();
        } catch (Exception e) {
            return null;
        }
    }

    /** Data e hora em UTC: ISO-8601 (com Z/offset) ou data-hora simples interpretada como UTC. */
    public static Instant instanteUtc(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String limpo = valor.trim();
        try {
            return Instant.parse(limpo);
        } catch (Exception e) { /* não é ISO com zona */ }
        try {
            return LocalDateTime.parse(limpo.replace(' ', 'T')).toInstant(ZoneOffset.UTC);
        } catch (Exception e) { /* tenta formatos com padrão */ }
        for (DateTimeFormatter f : DATAS_HORA) {
            try {
                return LocalDateTime.parse(limpo, f).toInstant(ZoneOffset.UTC);
            } catch (Exception e) { /* tenta o próximo formato */ }
        }
        return null;
    }

    public static int zeroSeNulo(Integer valor) { return valor == null ? 0 : valor; }
}
