package br.com.blockready.analytics;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.blockready.analytics.strategy.EstrategiaPadrao;
import br.com.blockready.analytics.strategy.PriorizacaoStrategy;
import br.com.blockready.dto.Analytics.CriCategoriaResponse;
import br.com.blockready.dto.Analytics.CriResponse;
import br.com.blockready.dto.Analytics.InsightResponse;
import br.com.blockready.dto.Analytics.RecomendacaoResponse;
import br.com.blockready.exception.ResourceNotFoundException;
import br.com.blockready.model.enums.CategoriaRecurso;
import br.com.blockready.model.enums.SituacaoCobertura;
import br.com.blockready.model.enums.StatusRecurso;
import br.com.blockready.repository.RecursoRepository;
import br.com.blockready.repository.RegistroDesastreRepository;
import br.com.blockready.repository.RegistroMeteorologicoRepository;
import br.com.blockready.repository.SetorCensitarioRepository;
import br.com.blockready.repository.SolicitacaoRepository;

/**
 * Os quatro insights estratégicos da Global Solution. Cada insight expõe pergunta, dados, fontes,
 * metodologia, resultado, interpretação, decisão recomendada e impacto — e informa quando a base
 * de dados é insuficiente, em vez de inventar números. Análises prioritariamente descritivas.
 */
@Service
public class InsightService {

    private final CriService criService;
    private final RegistroDesastreRepository desastres;
    private final SetorCensitarioRepository setores;
    private final RegistroMeteorologicoRepository meteorologicos;
    private final RecursoRepository recursos;
    private final SolicitacaoRepository solicitacoes;
    private final EstrategiaPadrao estrategiaPadrao;

    public InsightService(CriService criService, RegistroDesastreRepository desastres,
            SetorCensitarioRepository setores, RegistroMeteorologicoRepository meteorologicos,
            RecursoRepository recursos, SolicitacaoRepository solicitacoes, EstrategiaPadrao estrategiaPadrao) {
        this.criService = criService;
        this.desastres = desastres;
        this.setores = setores;
        this.meteorologicos = meteorologicos;
        this.recursos = recursos;
        this.solicitacoes = solicitacoes;
        this.estrategiaPadrao = estrategiaPadrao;
    }

    @Transactional(readOnly = true)
    public List<InsightResponse> todos() {
        return List.of(exposicaoHistorica(), coberturaRecursos(), monitoramentoProntidao(), alocacaoInteligente());
    }

    @Transactional(readOnly = true)
    public InsightResponse porCodigo(String codigo) {
        return switch (codigo) {
            case "historical-disaster-exposure" -> exposicaoHistorica();
            case "community-resource-coverage" -> coberturaRecursos();
            case "emergency-readiness-monitoring" -> monitoramentoProntidao();
            case "smart-resource-allocation" -> alocacaoInteligente();
            default -> throw new ResourceNotFoundException("Insight não encontrado: " + codigo);
        };
    }

    // ---- INSIGHT 1 — HISTORICAL DISASTER EXPOSURE ----

    public InsightResponse exposicaoHistorica() {
        List<Object[]> agregado = desastres.agregadoPorTipo();
        boolean suficiente = !agregado.isEmpty();

        List<Map<String, Object>> porTipo = new ArrayList<>();
        Map<CategoriaRecurso, BigDecimal> capacidade = capacidadePorCategoria();
        String tipoMaisFrequente = null;
        long maisAfetados = 0;
        for (Object[] linha : agregado) {
            String tipo = (String) linha[0];
            long ocorrencias = (Long) linha[1];
            long afetados = somaLong(linha[2]);
            if (tipoMaisFrequente == null) {
                tipoMaisFrequente = tipo;
            }
            maisAfetados = Math.max(maisAfetados, afetados);
            List<CategoriaRecurso> relacionadas = categoriasRelacionadas(tipo);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("tipoDesastre", tipo);
            item.put("ocorrencias", ocorrencias);
            item.put("pessoasAfetadas", afetados);
            item.put("desabrigados", somaLong(linha[3]));
            item.put("desalojados", somaLong(linha[4]));
            item.put("mortos", somaLong(linha[5]));
            item.put("prejuizoTotal", linha[6]);
            item.put("primeiraOcorrencia", linha[7]);
            item.put("ultimaOcorrencia", linha[8]);
            item.put("categoriasRelacionadas", relacionadas.stream().map(Enum::name).toList());
            Map<String, BigDecimal> capacidadeRelacionada = new LinkedHashMap<>();
            for (CategoriaRecurso c : relacionadas) {
                capacidadeRelacionada.put(c.name(), capacidade.getOrDefault(c, BigDecimal.ZERO));
            }
            item.put("capacidadeAtualRelacionada", capacidadeRelacionada);
            porTipo.add(item);
        }

        String interpretacao = suficiente
                ? "O histórico municipal indica \"" + tipoMaisFrequente + "\" como o cenário mais recorrente. "
                        + "Os tipos listados concentram as pessoas afetadas registradas e apontam quais categorias "
                        + "de recurso são mais exigidas em cada cenário."
                : "Sem registros do Atlas importados, não é possível identificar os cenários historicamente "
                        + "mais relevantes para o município.";
        String decisao = suficiente
                ? "Priorizar no planejamento os cenários com mais ocorrências e pessoas afetadas, conferindo se as "
                        + "categorias de recurso relacionadas a eles possuem capacidade cadastrada e validada."
                : "Importar o histórico do Atlas Digital de Desastres (tela Dados externos) antes de definir os "
                        + "cenários prioritários.";

        return new InsightResponse(
                "historical-disaster-exposure",
                "Insight 1 — Historical Disaster Exposure",
                "Quais tipos de desastre historicamente causaram maior impacto no município e merecem atenção no planejamento?",
                List.of("Ocorrências por tipo de desastre", "Pessoas afetadas, desabrigadas e desalojadas",
                        "Danos e prejuízos registrados", "Capacidade atual das categorias de recurso relacionadas"),
                List.of("Atlas Digital de Desastres (MIDR) — abrangência municipal",
                        "Recursos comunitários validados no BLOCKREADY"),
                "Agregação das ocorrências importadas do Atlas por tipo de desastre, somando danos humanos e "
                        + "prejuízos, e associação de cada tipo às categorias de recurso mais exigidas nesse cenário "
                        + "(relação descritiva por palavras-chave da tipologia).",
                suficiente, Map.of("porTipo", porTipo), interpretacao, decisao,
                "Direciona o planejamento preventivo para os cenários que mais afetaram pessoas no município, "
                        + "reduzindo o risco de preparar-se para o cenário errado.",
                List.of("Os registros do Atlas valem para o município inteiro e não são atribuídos a bairros específicos.",
                        "O histórico descreve o passado; não é uma previsão de novas ocorrências."),
                Instant.now());
    }

    // ---- INSIGHT 2 — COMMUNITY RESOURCE COVERAGE ----

    public InsightResponse coberturaRecursos() {
        long populacao = setores.populacaoTotal();
        long totalSetores = setores.count();
        long comGeometria = setores.countByGeometriaIsNotNull();
        boolean suficiente = totalSetores > 0;

        CriResponse cri = criService.calcularPadrao();
        List<Map<String, Object>> setoresDetalhe = new ArrayList<>();
        List<String> setoresDescobertos = new ArrayList<>();
        if (suficiente) {
            for (Object[] linha : setores.coberturaPorSetor(20)) {
                Map<String, Object> s = new LinkedHashMap<>();
                s.put("codigoSetor", linha[0]);
                s.put("municipio", linha[1]);
                s.put("uf", linha[2]);
                s.put("populacao", linha[3]);
                s.put("domicilios", linha[4]);
                s.put("recursosValidadosNaMalha", linha[5]);
                setoresDetalhe.add(s);
                if (linha[5] != null && ((Number) linha[5]).longValue() == 0) {
                    setoresDescobertos.add(linha[0] + " (" + linha[1] + ", pop. " + linha[3] + ")");
                }
            }
        }

        String interpretacao;
        if (!suficiente) {
            interpretacao = "Sem os setores censitários do IBGE importados, não é possível comparar a distribuição "
                    + "da população com a dos recursos.";
        } else if (setoresDescobertos.isEmpty()) {
            interpretacao = "Entre os setores mais populosos com malha importada, todos possuem ao menos um recurso "
                    + "validado em sua área. As categorias com cobertura PARCIAL ou INSUFICIENTE no CRI indicam "
                    + "onde a capacidade total ainda não acompanha a população.";
        } else {
            interpretacao = "Há setores populosos sem nenhum recurso validado dentro da malha ("
                    + String.join("; ", setoresDescobertos.subList(0, Math.min(5, setoresDescobertos.size())))
                    + "), indicando regiões com provável insuficiência de pontos de apoio.";
        }

        return new InsightResponse(
                "community-resource-coverage",
                "Insight 2 — Community Resource Coverage",
                "Os recursos comunitários estão distribuídos de maneira compatível com a população?",
                List.of("População e domicílios por setor censitário", "Malha geográfica dos setores (quando importada)",
                        "Localização e capacidade dos recursos validados", "CRI por categoria"),
                List.of("IBGE — Censo Demográfico 2022 (setores censitários)",
                        "Recursos comunitários validados no BLOCKREADY"),
                "Cruzamento espacial (PostGIS, SRID 4326): contagem de recursos validados contidos na malha de cada "
                        + "setor (ST_Contains) nos 20 setores mais populosos, combinada com o CRI por categoria "
                        + "para a população total.",
                suficiente,
                Map.of("populacaoTotal", populacao, "setores", totalSetores, "setoresComMalha", comGeometria,
                        "criPorCategoria", cri.categorias(), "setoresMaisPopulosos", setoresDetalhe,
                        "setoresSemRecursos", setoresDescobertos),
                interpretacao,
                suficiente
                        ? "Priorizar a implantação ou ampliação de pontos de apoio nos setores populosos sem recursos "
                                + "validados e nas categorias com cobertura insuficiente."
                        : "Importar os agregados do Censo 2022 (tela Dados externos), com a malha em WKT quando "
                                + "disponível, para habilitar esta análise.",
                "Evita que regiões populosas fiquem sem pontos de apoio próximos durante uma emergência.",
                List.of("Setores sem malha importada não entram no cruzamento espacial.",
                        "A análise considera recursos validados; pendentes não contam como confirmados."),
                Instant.now());
    }

    // ---- INSIGHT 3 — EMERGENCY READINESS MONITORING ----

    public InsightResponse monitoramentoProntidao() {
        Instant desde = Instant.now().minus(72, ChronoUnit.HOURS);
        List<Object[]> resumo = meteorologicos.resumoPorEstacao(desde);
        Instant ultimaMedicao = meteorologicos.ultimaMedicao();
        boolean temChuva = !resumo.isEmpty();
        boolean temBase = meteorologicos.count() > 0;

        List<Map<String, Object>> estacoes = new ArrayList<>();
        BigDecimal maiorAcumulado = BigDecimal.ZERO;
        for (Object[] linha : resumo) {
            Map<String, Object> e = new LinkedHashMap<>();
            e.put("estacao", linha[0]);
            e.put("nome", linha[1]);
            e.put("municipio", linha[2]);
            e.put("acumulado72hMm", linha[6]);
            e.put("maximoMm", linha[7]);
            e.put("ultimaMedicao", linha[8]);
            estacoes.add(e);
            if (linha[6] instanceof BigDecimal acumulado && acumulado.compareTo(maiorAcumulado) > 0) {
                maiorAcumulado = acumulado;
            }
        }

        Instant limiteDesatualizacao = Instant.now().minus(7, ChronoUnit.DAYS);
        long recursosDesatualizados = recursos.countByStatusAndAtualizadoEmBefore(StatusRecurso.VALIDADO,
                limiteDesatualizacao);

        Map<String, Long> abertasPorCategoria = new LinkedHashMap<>();
        for (Object[] linha : solicitacoes.abertasPorCategoria()) {
            abertasPorCategoria.put(((CategoriaRecurso) linha[0]).name(), (Long) linha[1]);
        }

        boolean dadosMeteorologicosAtuais = ultimaMedicao != null && ultimaMedicao.isAfter(desde);
        String interpretacao;
        if (!temBase) {
            interpretacao = "Sem medições do CEMADEN importadas, o acompanhamento meteorológico fica indisponível; "
                    + "restam as solicitações abertas e a atualização cadastral como sinais de prontidão.";
        } else if (!dadosMeteorologicosAtuais) {
            interpretacao = "As medições importadas do CEMADEN não cobrem as últimas 72 horas (última: "
                    + ultimaMedicao + "): os dados estão desatualizados para monitoramento em tempo real, "
                    + "mas seguem úteis como histórico.";
        } else {
            interpretacao = "Maior acumulado de chuva em 72 h: " + maiorAcumulado.stripTrailingZeros().toPlainString()
                    + " mm. Chuva observada e disponibilidade de recursos são apresentadas lado a lado para "
                    + "orientar verificações preventivas — a coincidência de períodos não demonstra relação causal.";
        }

        return new InsightResponse(
                "emergency-readiness-monitoring",
                "Insight 3 — Emergency Readiness Monitoring",
                "Como as condições meteorológicas observadas se relacionam com a disponibilidade atual dos recursos?",
                List.of("Precipitação por estação (acumulado e máximo em 72 h)", "Última medição importada",
                        "Recursos validados sem atualização há mais de 7 dias", "Solicitações em aberto por categoria"),
                List.of("CEMADEN — monitoramento pluviométrico (medições valem para o ponto da estação; horários em UTC)",
                        "Registros internos do BLOCKREADY (recursos e solicitações)"),
                "Soma e máximo da precipitação por estação na janela de 72 h; contagem de recursos validados sem "
                        + "atualização recente; solicitações em aberto agrupadas pela categoria da necessidade. "
                        + "Análise descritiva: nenhuma relação causal é inferida entre chuva e indisponibilidade.",
                temBase,
                Map.of("estacoes", estacoes, "ultimaMedicao", ultimaMedicao == null ? "—" : ultimaMedicao,
                        "recursosValidadosDesatualizados7d", recursosDesatualizados,
                        "solicitacoesAbertasPorCategoria", abertasPorCategoria,
                        "medicoesCobremUltimas72h", dadosMeteorologicosAtuais),
                interpretacao,
                temChuva || recursosDesatualizados > 0
                        ? "Verificar preventivamente a disponibilidade dos recursos próximos às estações com maior "
                                + "acumulado e atualizar os cadastros parados há mais de 7 dias."
                        : "Manter a rotina de atualização dos recursos e importar medições recentes do CEMADEN "
                                + "para acompanhar as condições de chuva.",
                "Permite mobilização preventiva e evita decisões baseadas em informações desatualizadas.",
                List.of("Medições valem para a localização da estação, não para o município inteiro.",
                        "Chuva e indisponibilidade no mesmo período não implicam relação causal."),
                Instant.now());
    }

    // ---- INSIGHT 4 — SMART RESOURCE ALLOCATION ----

    public InsightResponse alocacaoInteligente() {
        CriResponse cri = criService.calcularPadrao();
        boolean suficiente = cri.dadosSuficientes();
        List<RecomendacaoResponse> recomendacoes = estrategiaPadrao.analyze(cri);

        List<Map<String, Object>> deficits = new ArrayList<>();
        for (CriCategoriaResponse c : cri.categorias()) {
            Map<String, Object> d = new LinkedHashMap<>();
            d.put("categoria", c.categoria().name());
            d.put("unidade", c.unidade());
            d.put("oferta", c.capacidadeDisponivel());
            d.put("demandaEstimada", c.demandaEstimada());
            d.put("deficit", c.insuficiencia());
            d.put("situacao", c.situacao().name());
            deficits.add(d);
        }

        List<String> areasPrioritarias = new ArrayList<>();
        for (Object[] linha : setores.coberturaPorSetor(20)) {
            if (linha[5] != null && ((Number) linha[5]).longValue() == 0) {
                areasPrioritarias.add(linha[0] + " — " + linha[1] + " (pop. " + linha[3] + ")");
            }
        }

        String tipoHistorico = desastres.agregadoPorTipo().stream().findFirst()
                .map(l -> (String) l[0]).orElse(null);

        return new InsightResponse(
                "smart-resource-allocation",
                "Insight 4 — Smart Resource Allocation",
                "Como os recursos existentes podem ser mobilizados para reduzir os déficits mais relevantes?",
                List.of("Demanda estimada por categoria (população IBGE × parâmetros de planejamento)",
                        "Oferta disponível (recursos validados)", "Déficit por categoria",
                        "Setores populosos sem recursos na malha", "Cenário historicamente mais frequente"),
                List.of("IBGE — Censo Demográfico 2022", "Atlas Digital de Desastres (MIDR)",
                        "Recursos comunitários validados no BLOCKREADY"),
                "CRI por categoria com parâmetros padrão e priorização pelo padrão Strategy ("
                        + estrategiaPadrao.key() + "): urgência = peso da categoria × (1 − cobertura). "
                        + "Critérios transparentes, exibidos em cada recomendação.",
                suficiente,
                Map.of("deficitsPorCategoria", deficits, "recomendacoes", recomendacoes,
                        "areasPrioritarias", areasPrioritarias,
                        "cenarioHistoricoMaisFrequente", tipoHistorico == null ? "—" : tipoHistorico,
                        "estrategia", estrategiaPadrao.key()),
                suficiente
                        ? "As categorias no topo da priorização concentram os maiores déficits ponderados; as áreas "
                                + "prioritárias listam setores populosos ainda sem pontos de apoio validados."
                        : "Sem a base do IBGE importada, a demanda não pode ser estimada e a priorização fica "
                                + "indisponível.",
                suficiente
                        ? "Mobilizar capacidade adicional seguindo a ordem de priorização e avaliar novos pontos de "
                                + "apoio nas áreas prioritárias; para um cenário específico, usar a simulação "
                                + "(Emergency Scenario), que aplica a estratégia do tipo de emergência."
                        : "Importar os dados do IBGE para habilitar a priorização.",
                "Concentra a mobilização onde o déficit ponderado é maior, com critérios auditáveis.",
                List.of("As recomendações dependem da qualidade e atualização dos dados cadastrados.",
                        "Os parâmetros de demanda são premissas de planejamento configuráveis."),
                Instant.now());
    }

    // ---- apoio ----

    private Map<CategoriaRecurso, BigDecimal> capacidadePorCategoria() {
        Map<CategoriaRecurso, BigDecimal> mapa = new EnumMap<>(CategoriaRecurso.class);
        for (CriCategoriaResponse c : criService.calcularPadrao().categorias()) {
            mapa.put(c.categoria(), c.capacidadeDisponivel());
        }
        return mapa;
    }

    /** Relação descritiva tipologia do Atlas -> categorias de recurso mais exigidas no cenário. */
    static List<CategoriaRecurso> categoriasRelacionadas(String tipoDesastre) {
        String t = java.text.Normalizer.normalize(tipoDesastre.toLowerCase(Locale.ROOT),
                java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        if (t.contains("inunda") || t.contains("enxurrada") || t.contains("alagamento") || t.contains("enchente")) {
            return List.of(CategoriaRecurso.ABRIGO, CategoriaRecurso.AGUA, CategoriaRecurso.SAUDE);
        }
        if (t.contains("estiagem") || t.contains("seca")) {
            return List.of(CategoriaRecurso.AGUA, CategoriaRecurso.ALIMENTACAO);
        }
        if (t.contains("vendaval") || t.contains("granizo") || t.contains("tempestade") || t.contains("tornado")
                || t.contains("ciclone")) {
            return List.of(CategoriaRecurso.ABRIGO, CategoriaRecurso.ENERGIA, CategoriaRecurso.SAUDE);
        }
        if (t.contains("deslizamento") || t.contains("movimento de massa")) {
            return List.of(CategoriaRecurso.ABRIGO, CategoriaRecurso.SAUDE, CategoriaRecurso.COMUNICACAO);
        }
        if (t.contains("incendio") || t.contains("fogo")) {
            return List.of(CategoriaRecurso.SAUDE, CategoriaRecurso.ABRIGO, CategoriaRecurso.AGUA);
        }
        if (t.contains("epidemia") || t.contains("doenca")) {
            return List.of(CategoriaRecurso.SAUDE, CategoriaRecurso.AGUA);
        }
        return List.of(CategoriaRecurso.ABRIGO, CategoriaRecurso.AGUA, CategoriaRecurso.ALIMENTACAO);
    }

    private long somaLong(Object valor) {
        return valor == null ? 0 : ((Number) valor).longValue();
    }

    /** Data de referência exibida nas análises históricas. */
    public LocalDate hoje() { return LocalDate.now(); }
}
