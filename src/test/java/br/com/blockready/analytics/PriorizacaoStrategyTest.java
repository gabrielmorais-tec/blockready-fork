package br.com.blockready.analytics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import br.com.blockready.analytics.strategy.EstrategiaEnchente;
import br.com.blockready.analytics.strategy.EstrategiaInterrupcaoEnergia;
import br.com.blockready.analytics.strategy.EstrategiaPadrao;
import br.com.blockready.dto.Analytics.CriCategoriaResponse;
import br.com.blockready.dto.Analytics.CriResponse;
import br.com.blockready.dto.Analytics.RecomendacaoResponse;
import br.com.blockready.model.enums.CategoriaRecurso;
import br.com.blockready.model.enums.SituacaoCobertura;
import br.com.blockready.model.enums.TipoEmergencia;
import br.com.blockready.service.CenarioService;

class PriorizacaoStrategyTest {

    private CriCategoriaResponse cat(CategoriaRecurso c, String percentual, SituacaoCobertura situacao,
            String insuficiencia) {
        return new CriCategoriaResponse(c, "un", BigDecimal.TEN,
                BigDecimal.TEN, percentual == null ? null : new BigDecimal(percentual), situacao,
                insuficiencia == null ? null : new BigDecimal(insuficiencia), 1, 0, "teste", Instant.now(), null);
    }

    private CriResponse cri(List<CriCategoriaResponse> categorias) {
        return new CriResponse(Instant.now(), 1000, 10, BigDecimal.valueOf(100), 1000, 72, CriService.METODO,
                Map.of(), categorias, List.of(), List.of(), true);
    }

    @Test
    void enchentePrioritizaAbrigoQuandoCoberturasIguais() {
        CriResponse cri = cri(List.of(
                cat(CategoriaRecurso.ENERGIA, "20", SituacaoCobertura.INSUFICIENTE, "80"),
                cat(CategoriaRecurso.ABRIGO, "20", SituacaoCobertura.INSUFICIENTE, "80")));

        List<RecomendacaoResponse> r = new EstrategiaEnchente().analyze(cri);

        assertEquals(CategoriaRecurso.ABRIGO, r.get(0).categoria());
        assertEquals(1, r.get(0).prioridade());
        assertTrue(r.get(0).justificativa().contains("Peso da categoria"));
    }

    @Test
    void interrupcaoDeEnergiaInverteAPrioridade() {
        CriResponse cri = cri(List.of(
                cat(CategoriaRecurso.ENERGIA, "20", SituacaoCobertura.INSUFICIENTE, "80"),
                cat(CategoriaRecurso.ABRIGO, "20", SituacaoCobertura.INSUFICIENTE, "80")));

        List<RecomendacaoResponse> r = new EstrategiaInterrupcaoEnergia().analyze(cri);

        assertEquals(CategoriaRecurso.ENERGIA, r.get(0).categoria());
    }

    @Test
    void coberturaTotalNaoGeraRecomendacaoECemPorCentoNaoEntra() {
        CriResponse cri = cri(List.of(
                cat(CategoriaRecurso.AGUA, "120", SituacaoCobertura.SUFICIENTE, "0"),
                cat(CategoriaRecurso.SAUDE, "40", SituacaoCobertura.INSUFICIENTE, "60")));

        List<RecomendacaoResponse> r = new EstrategiaPadrao().analyze(cri);

        assertEquals(1, r.size());
        assertEquals(CategoriaRecurso.SAUDE, r.get(0).categoria());
    }

    @Test
    void semDadosViraRecomendacaoDeColeta() {
        CriResponse cri = cri(List.of(cat(CategoriaRecurso.COMUNICACAO, null, SituacaoCobertura.SEM_DADOS, null)));

        List<RecomendacaoResponse> r = new EstrategiaPadrao().analyze(cri);

        assertEquals(1, r.size());
        assertNull(r.get(0).deficit());
        assertTrue(r.get(0).acao().toLowerCase().contains("levantar dados"));
    }

    @Test
    void cadaTipoDeEmergenciaTemEstrategiaPropria() {
        assertEquals("priorizacao-enchente", CenarioService.estrategiaPara(TipoEmergencia.ENCHENTE));
        assertEquals("priorizacao-tempestade", CenarioService.estrategiaPara(TipoEmergencia.TEMPESTADE_INTENSA));
        assertEquals("priorizacao-interrupcao-energia",
                CenarioService.estrategiaPara(TipoEmergencia.INTERRUPCAO_ENERGIA));
        assertEquals("priorizacao-interrupcao-servicos",
                CenarioService.estrategiaPara(TipoEmergencia.INTERRUPCAO_SERVICOS));
    }
}
