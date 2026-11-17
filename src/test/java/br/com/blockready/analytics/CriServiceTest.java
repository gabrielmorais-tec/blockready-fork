package br.com.blockready.analytics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.com.blockready.dto.Analytics.CriCategoriaResponse;
import br.com.blockready.dto.Analytics.CriResponse;
import br.com.blockready.model.enums.CategoriaRecurso;
import br.com.blockready.model.enums.SituacaoCobertura;
import br.com.blockready.repository.RecursoRepository;
import br.com.blockready.repository.SetorCensitarioRepository;

class CriServiceTest {

    private RecursoRepository recursos;
    private SetorCensitarioRepository setores;
    private CriService service;

    @BeforeEach
    void preparar() {
        recursos = mock(RecursoRepository.class);
        setores = mock(SetorCensitarioRepository.class);
        service = new CriService(recursos, setores);
    }

    private CriCategoriaResponse categoria(CriResponse cri, CategoriaRecurso c) {
        return cri.categorias().stream().filter(x -> x.categoria() == c).findFirst().orElseThrow();
    }

    @Test
    void semPopulacaoImportadaTodasAsCategoriasFicamSemDados() {
        when(setores.populacaoTotal()).thenReturn(0L);
        when(setores.count()).thenReturn(0L);
        when(recursos.capacidadePorCategoriaEUnidade(any())).thenReturn(List.of());

        CriResponse cri = service.calcularPadrao();

        assertFalse(cri.dadosSuficientes());
        assertEquals(6, cri.categorias().size());
        cri.categorias().forEach(c -> {
            assertEquals(SituacaoCobertura.SEM_DADOS, c.situacao());
            assertNull(c.percentual());
        });
    }

    @Test
    void calculaCoberturaEInsuficienciaComUnidadeCompativel() {
        when(setores.populacaoTotal()).thenReturn(1000L);
        when(setores.count()).thenReturn(10L);
        // 4500 litros disponíveis em 2 recursos; demanda 72h = 9000 L para 1000 pessoas.
        when(recursos.capacidadePorCategoriaEUnidade(any())).thenReturn(List.<Object[]>of(
                new Object[] { CategoriaRecurso.AGUA, "litros", new BigDecimal("4500"), 2L, Instant.now() }));

        CriCategoriaResponse agua = categoria(service.calcularPadrao(), CategoriaRecurso.AGUA);

        assertEquals(0, new BigDecimal("50.00").compareTo(agua.percentual()));
        assertEquals(SituacaoCobertura.PARCIAL, agua.situacao());
        assertEquals(0, new BigDecimal("4500.00").compareTo(agua.insuficiencia()));
        assertEquals(2, agua.recursosConsiderados());
    }

    @Test
    void capacidadeZeroComDemandaPositivaEhInsuficiente() {
        when(setores.populacaoTotal()).thenReturn(1000L);
        when(setores.count()).thenReturn(10L);
        when(recursos.capacidadePorCategoriaEUnidade(any())).thenReturn(List.of());

        CriCategoriaResponse abrigo = categoria(service.calcularPadrao(), CategoriaRecurso.ABRIGO);

        assertEquals(0, BigDecimal.ZERO.compareTo(abrigo.percentual()));
        assertEquals(SituacaoCobertura.INSUFICIENTE, abrigo.situacao());
        assertEquals(0, new BigDecimal("300.00").compareTo(abrigo.demandaEstimada()));
    }

    @Test
    void unidadeIncompativelEhExcluidaENaoSomada() {
        when(setores.populacaoTotal()).thenReturn(100L);
        when(setores.count()).thenReturn(1L);
        when(recursos.capacidadePorCategoriaEUnidade(any())).thenReturn(List.<Object[]>of(
                new Object[] { CategoriaRecurso.AGUA, "litros", new BigDecimal("100"), 1L, Instant.now() },
                new Object[] { CategoriaRecurso.AGUA, "caixas", new BigDecimal("999999"), 3L, Instant.now() }));

        CriCategoriaResponse agua = categoria(service.calcularPadrao(), CategoriaRecurso.AGUA);

        assertEquals(0, new BigDecimal("100.00").compareTo(agua.capacidadeDisponivel()));
        assertEquals(1, agua.recursosConsiderados());
        assertEquals(3, agua.recursosExcluidosPorUnidade());
        assertTrue(agua.observacao().contains("unidade de medida incompatível"));
    }

    @Test
    void demandaZeroNaoDivideNemClassificaComoSuficiente() {
        when(setores.populacaoTotal()).thenReturn(1000L);
        when(setores.count()).thenReturn(10L);
        when(recursos.capacidadePorCategoriaEUnidade(any())).thenReturn(List.of());
        ParametrosDemanda zerado = new ParametrosDemanda(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.valueOf(90), BigDecimal.valueOf(50));

        CriResponse cri = service.calcular(zerado, BigDecimal.valueOf(100), 72, null);

        cri.categorias().forEach(c -> {
            assertNull(c.percentual());
            assertEquals(SituacaoCobertura.SEM_DADOS, c.situacao());
        });
    }

    @Test
    void coberturaCemPorCentoVemComLimitacaoDeNaoGarantia() {
        when(setores.populacaoTotal()).thenReturn(100L);
        when(setores.count()).thenReturn(1L);
        when(recursos.capacidadePorCategoriaEUnidade(any())).thenReturn(List.<Object[]>of(
                new Object[] { CategoriaRecurso.AGUA, "litros", new BigDecimal("900"), 1L, Instant.now() }));

        CriResponse cri = service.calcularPadrao();

        assertEquals(SituacaoCobertura.SUFICIENTE, categoria(cri, CategoriaRecurso.AGUA).situacao());
        assertTrue(cri.limitacoes().stream().anyMatch(l -> l.contains("não garantia de segurança")));
    }
}
