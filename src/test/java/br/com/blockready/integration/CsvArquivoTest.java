package br.com.blockready.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import br.com.blockready.exception.BusinessException;

class CsvArquivoTest {

    @Test
    void lePontoEVirgulaComCabecalhoNormalizado() {
        CsvArquivo csv = CsvArquivo.ler("CD_SETOR;Município;V0001\n410690205000001;Curitiba;123\n"
                .getBytes(StandardCharsets.UTF_8));
        assertEquals(';', csv.delimitador());
        assertEquals(1, csv.linhas().size());
        assertTrue(csv.possuiColuna("cd_setor"));
        assertTrue(csv.possuiColuna("municipio"));
        assertEquals("Curitiba", csv.linhas().get(0).texto("municipio"));
        assertEquals("123", csv.linhas().get(0).texto("v0001"));
    }

    @Test
    void leVirgulaComCamposEntreAspas() {
        CsvArquivo csv = CsvArquivo.ler("nome,descricao\n\"Ponto, central\",\"com \"\"aspas\"\"\"\n"
                .getBytes(StandardCharsets.UTF_8));
        assertEquals(',', csv.delimitador());
        assertEquals("Ponto, central", csv.linhas().get(0).texto("nome"));
        assertEquals("com \"aspas\"", csv.linhas().get(0).texto("descricao"));
    }

    @Test
    void removeBomEIgnoraLinhasVazias() {
        CsvArquivo csv = CsvArquivo.ler("﻿a;b\n1;2\n\n3;4\n".getBytes(StandardCharsets.UTF_8));
        assertTrue(csv.possuiColuna("a"));
        assertEquals(2, csv.linhas().size());
    }

    @Test
    void textoDevolveNuloQuandoColunaAusenteOuVazia() {
        CsvArquivo csv = CsvArquivo.ler("a;b\n1;\n".getBytes(StandardCharsets.UTF_8));
        assertNull(csv.linhas().get(0).texto("b"));
        assertNull(csv.linhas().get(0).texto("inexistente"));
        assertEquals("1", csv.linhas().get(0).texto("inexistente", "a"));
    }

    @Test
    void arquivoVazioEhRejeitado() {
        assertThrows(BusinessException.class, () -> CsvArquivo.ler(new byte[0]));
        assertThrows(BusinessException.class, () -> CsvArquivo.ler(null));
    }
}
