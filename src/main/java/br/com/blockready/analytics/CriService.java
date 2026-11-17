package br.com.blockready.analytics;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.blockready.dto.Analytics.CriCategoriaResponse;
import br.com.blockready.dto.Analytics.CriResponse;
import br.com.blockready.model.enums.CategoriaRecurso;
import br.com.blockready.model.enums.SituacaoCobertura;
import br.com.blockready.model.enums.StatusRecurso;
import br.com.blockready.repository.RecursoRepository;
import br.com.blockready.repository.SetorCensitarioRepository;

/**
 * Community Resilience Index: CRI = (capacidade disponível / demanda estimada) × 100, por categoria.
 *
 * <p>Capacidade: soma dos recursos VALIDADOS cuja unidade de medida é compatível com a escala da
 * categoria (recursos em unidades incompatíveis são excluídos e informados — escalas não se misturam).
 * Demanda: população dos setores censitários do IBGE × parâmetros configuráveis de planejamento.
 * Divisão por zero, demanda zero e ausência de dados viram {@code SEM_DADOS}, nunca erro.
 * Cobertura de 100% indica compatibilidade com a demanda estimada, não garantia de segurança.</p>
 */
@Service
public class CriService {

    public static final String METODO = "cri-capacidade-sobre-demanda";

    private final RecursoRepository recursos;
    private final SetorCensitarioRepository setores;

    public CriService(RecursoRepository recursos, SetorCensitarioRepository setores) {
        this.recursos = recursos;
        this.setores = setores;
    }

    /** Cálculo com todos os padrões (painel): população inteira, horizonte de 72 h. */
    @Transactional(readOnly = true)
    public CriResponse calcularPadrao() {
        return calcular(ParametrosDemanda.padrao(), BigDecimal.valueOf(100), 72, null);
    }

    @Transactional(readOnly = true)
    public CriResponse calcular(ParametrosDemanda parametros, BigDecimal percentualAfetados, int horizonteHoras,
            Collection<CategoriaRecurso> categoriasFiltro) {
        long populacaoBase = setores.populacaoTotal();
        long setoresConsiderados = setores.count();
        long populacaoAfetada = BigDecimal.valueOf(populacaoBase)
                .multiply(percentualAfetados)
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP).longValue();

        Map<CategoriaRecurso, Acumulado> acumulados = acumularCapacidades();

        List<String> limitacoes = new ArrayList<>();
        boolean dadosSuficientes = populacaoBase > 0;
        if (!dadosSuficientes) {
            limitacoes.add("Não há setores censitários importados (IBGE): a demanda não pode ser estimada "
                    + "e as categorias aparecem como SEM_DADOS.");
        }
        limitacoes.add("Os parâmetros de demanda são premissas de planejamento configuráveis, não dados oficiais.");
        limitacoes.add("Cobertura de 100% indica capacidade compatível com a demanda estimada, "
                + "não garantia de segurança da comunidade.");

        List<CriCategoriaResponse> categorias = new ArrayList<>();
        for (CategoriaRecurso categoria : CategoriaRecurso.values()) {
            if (categoriasFiltro != null && !categoriasFiltro.isEmpty() && !categoriasFiltro.contains(categoria)) {
                continue;
            }
            categorias.add(calcularCategoria(categoria, acumulados.get(categoria), populacaoBase,
                    populacaoAfetada, horizonteHoras, parametros));
        }

        List<String> fontes = List.of(
                "Recursos comunitários validados no BLOCKREADY (capacidade disponível)",
                "IBGE — Censo Demográfico 2022, população por setor censitário (demanda)");

        return new CriResponse(Instant.now(), populacaoBase, setoresConsiderados, percentualAfetados,
                populacaoAfetada, horizonteHoras, METODO, parametros.comoMapa(), categorias, fontes,
                limitacoes, dadosSuficientes);
    }

    private CriCategoriaResponse calcularCategoria(CategoriaRecurso categoria, Acumulado acumulado,
            long populacaoBase, long populacaoAfetada, int horizonteHoras, ParametrosDemanda parametros) {
        BigDecimal capacidade = acumulado != null ? acumulado.capacidade : BigDecimal.ZERO;
        int considerados = acumulado != null ? acumulado.considerados : 0;
        int excluidos = acumulado != null ? acumulado.excluidos : 0;
        Instant ultimaAtualizacao = acumulado != null ? acumulado.ultimaAtualizacao : null;
        String unidade = ParametrosDemanda.unidadePadrao(categoria);
        String fonte = "Recursos validados do BLOCKREADY + população IBGE (setores censitários)";

        if (populacaoBase <= 0) {
            return new CriCategoriaResponse(categoria, unidade, capacidade, BigDecimal.ZERO, null,
                    SituacaoCobertura.SEM_DADOS, null, considerados, excluidos, fonte, ultimaAtualizacao,
                    "Sem dados do IBGE importados: não é possível estimar a demanda.");
        }

        BigDecimal demanda = parametros.demandaEstimada(categoria, populacaoAfetada, horizonteHoras);
        if (demanda.signum() <= 0) {
            // Demanda zero (parâmetro zerado ou população afetada nula): divisão indefinida, sem percentual.
            return new CriCategoriaResponse(categoria, unidade, capacidade, demanda, null,
                    SituacaoCobertura.SEM_DADOS, null, considerados, excluidos, fonte, ultimaAtualizacao,
                    "Demanda estimada igual a zero para os parâmetros informados: cobertura não calculável.");
        }

        BigDecimal percentual = capacidade.multiply(BigDecimal.valueOf(100))
                .divide(demanda, 2, RoundingMode.HALF_UP);
        SituacaoCobertura situacao = SituacaoCobertura.classificar(percentual,
                parametros.limiarSuficiente(), parametros.limiarParcial());
        BigDecimal insuficiencia = demanda.subtract(capacidade).max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
        String observacao = excluidos > 0
                ? excluidos + " recurso(s) excluído(s) do cálculo por unidade de medida incompatível com \""
                        + unidade + "\"."
                : null;
        return new CriCategoriaResponse(categoria, unidade, capacidade.setScale(2, RoundingMode.HALF_UP),
                demanda, percentual, situacao, insuficiencia, considerados, excluidos, fonte,
                ultimaAtualizacao, observacao);
    }

    private Map<CategoriaRecurso, Acumulado> acumularCapacidades() {
        Map<CategoriaRecurso, Acumulado> mapa = new EnumMap<>(CategoriaRecurso.class);
        for (Object[] linha : recursos.capacidadePorCategoriaEUnidade(StatusRecurso.VALIDADO)) {
            CategoriaRecurso categoria = (CategoriaRecurso) linha[0];
            String unidade = (String) linha[1];
            BigDecimal soma = (BigDecimal) linha[2];
            int quantidade = ((Long) linha[3]).intValue();
            Instant atualizadoEm = (Instant) linha[4];
            Acumulado acumulado = mapa.computeIfAbsent(categoria, c -> new Acumulado());
            if (ParametrosDemanda.unidadeCompativel(categoria, unidade)) {
                acumulado.capacidade = acumulado.capacidade.add(soma);
                acumulado.considerados += quantidade;
                if (acumulado.ultimaAtualizacao == null || atualizadoEm.isAfter(acumulado.ultimaAtualizacao)) {
                    acumulado.ultimaAtualizacao = atualizadoEm;
                }
            } else {
                acumulado.excluidos += quantidade;
            }
        }
        return mapa;
    }

    private static final class Acumulado {
        private BigDecimal capacidade = BigDecimal.ZERO;
        private int considerados;
        private int excluidos;
        private Instant ultimaAtualizacao;
    }
}
