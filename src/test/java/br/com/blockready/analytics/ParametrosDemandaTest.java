package br.com.blockready.analytics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import br.com.blockready.dto.Analytics.ParametrosDemandaRequest;
import br.com.blockready.model.enums.CategoriaRecurso;

class ParametrosDemandaTest {

    @Test
    void demandaDeAguaEscalaComHorizonteEPessoas() {
        ParametrosDemanda p = ParametrosDemanda.padrao();
        // 1000 pessoas × 3 L/dia × 3 dias (72 h) = 9000 L
        assertEquals(0, new BigDecimal("9000.00")
                .compareTo(p.demandaEstimada(CategoriaRecurso.AGUA, 1000, 72)));
        // Em 24 h, um terço.
        assertEquals(0, new BigDecimal("3000.00")
                .compareTo(p.demandaEstimada(CategoriaRecurso.AGUA, 1000, 24)));
    }

    @Test
    void demandaDeAbrigoUsaPercentualSemEscalarPorHora() {
        ParametrosDemanda p = ParametrosDemanda.padrao();
        // 30% de 1000 pessoas = 300 vagas, independentemente do horizonte.
        assertEquals(0, new BigDecimal("300.00")
                .compareTo(p.demandaEstimada(CategoriaRecurso.ABRIGO, 1000, 72)));
        assertEquals(0, new BigDecimal("300.00")
                .compareTo(p.demandaEstimada(CategoriaRecurso.ABRIGO, 1000, 24)));
    }

    @Test
    void requisicaoSobrepoeApenasOsCamposInformados() {
        ParametrosDemanda p = ParametrosDemanda.de(new ParametrosDemandaRequest(
                BigDecimal.valueOf(5), null, null, null, null, null, null, null));
        assertEquals(BigDecimal.valueOf(5), p.aguaLitrosPorPessoaDia());
        assertEquals(ParametrosDemanda.padrao().refeicoesPorPessoaDia(), p.refeicoesPorPessoaDia());
    }

    @Test
    void unidadesIncompativeisNaoEntramNaEscalaDaCategoria() {
        assertTrue(ParametrosDemanda.unidadeCompativel(CategoriaRecurso.AGUA, "Litros"));
        assertTrue(ParametrosDemanda.unidadeCompativel(CategoriaRecurso.ABRIGO, "vagas"));
        assertFalse(ParametrosDemanda.unidadeCompativel(CategoriaRecurso.AGUA, "caixas"));
        assertFalse(ParametrosDemanda.unidadeCompativel(CategoriaRecurso.ALIMENTACAO, "kg"));
        assertFalse(ParametrosDemanda.unidadeCompativel(CategoriaRecurso.ENERGIA, null));
    }
}
