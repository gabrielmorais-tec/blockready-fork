package br.com.blockready.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.blockready.analytics.CriService;
import br.com.blockready.analytics.ParametrosDemanda;
import br.com.blockready.analytics.strategy.PriorizacaoStrategy;
import br.com.blockready.dto.Analytics.CenarioRequest;
import br.com.blockready.dto.Analytics.CenarioResponse;
import br.com.blockready.dto.Analytics.CenarioResumoResponse;
import br.com.blockready.dto.Analytics.CriResponse;
import br.com.blockready.dto.Analytics.RecomendacaoResponse;
import br.com.blockready.dto.Responses.PageResponse;
import br.com.blockready.exception.ResourceNotFoundException;
import br.com.blockready.model.entity.CenarioEmergencia;
import br.com.blockready.model.entity.IndicadorCobertura;
import br.com.blockready.model.enums.TipoEmergencia;
import br.com.blockready.repository.CenarioEmergenciaRepository;
import br.com.blockready.repository.IndicadorCoberturaRepository;
import br.com.blockready.repository.UsuarioRepository;
import br.com.blockready.security.UsuarioPrincipal;

/**
 * Simulação de cenários de emergência (planejamento de até 72 h): consulta os recursos, estima a
 * demanda pelos parâmetros configurados, calcula a cobertura por categoria e aplica a estratégia
 * de priorização do tipo de emergência (padrão Strategy). Ferramenta de planejamento, não previsão.
 */
@Service
public class CenarioService {

    private final CriService criService;
    private final Map<String, PriorizacaoStrategy> estrategias;
    private final CenarioEmergenciaRepository cenarios;
    private final IndicadorCoberturaRepository indicadores;
    private final UsuarioRepository usuarios;
    private final AuditoriaService auditoria;
    private final ObjectMapper json;

    public CenarioService(CriService criService, List<PriorizacaoStrategy> listaEstrategias,
            CenarioEmergenciaRepository cenarios, IndicadorCoberturaRepository indicadores,
            UsuarioRepository usuarios, AuditoriaService auditoria, ObjectMapper json) {
        this.criService = criService;
        this.estrategias = listaEstrategias.stream()
                .collect(Collectors.toMap(PriorizacaoStrategy::key, Function.identity()));
        this.cenarios = cenarios;
        this.indicadores = indicadores;
        this.usuarios = usuarios;
        this.auditoria = auditoria;
        this.json = json;
    }

    /** Estratégia aplicada a cada tipo de emergência. */
    public static String estrategiaPara(TipoEmergencia tipo) {
        return switch (tipo) {
            case ENCHENTE -> "priorizacao-enchente";
            case TEMPESTADE_INTENSA -> "priorizacao-tempestade";
            case INTERRUPCAO_ENERGIA -> "priorizacao-interrupcao-energia";
            case INTERRUPCAO_SERVICOS -> "priorizacao-interrupcao-servicos";
        };
    }

    /** Executa a simulação sem persistir nada. */
    @Transactional(readOnly = true)
    public CenarioResponse simular(CenarioRequest req) {
        return calcular(req, null, null, null);
    }

    /** Executa a simulação e salva o cenário com seus indicadores, para comparação posterior. */
    @Transactional
    public CenarioResponse salvar(CenarioRequest req, UsuarioPrincipal ator) {
        CenarioResponse calculado = calcular(req, null, null, null);
        CenarioEmergencia cenario = new CenarioEmergencia(req.nome().trim(), req.tipo(), req.horizonteHoras(),
                req.percentualAfetados(), calculado.estrategia(), escrever(req), escrever(calculado),
                usuarios.getReferenceById(ator.getId()));
        cenario = cenarios.save(cenario);
        for (var categoria : calculado.cri().categorias()) {
            indicadores.save(new IndicadorCobertura(cenario, categoria.categoria(),
                    categoria.capacidadeDisponivel(), categoria.demandaEstimada(), categoria.percentual(),
                    categoria.situacao(), categoria.unidade(), categoria.fonteDados(), calculado.estrategia()));
        }
        auditoria.registrar(ator.getId(), "CENARIO_SALVO", "CenarioEmergencia", cenario.getId(),
                req.tipo() + " / " + req.horizonteHoras() + "h / " + req.percentualAfetados() + "% afetados");
        return calcular(req, cenario.getId(), cenario.getCriadoPor().getNome(), cenario.getCriadoEm());
    }

    @Transactional(readOnly = true)
    public PageResponse<CenarioResumoResponse> listar(int pagina, int tamanho) {
        var pageable = PageResponse.pageable(pagina, tamanho, Sort.unsorted());
        return PageResponse.de(cenarios.findByOrderByCriadoEmDesc(pageable)
                .map(c -> new CenarioResumoResponse(c.getId(), c.getNome(), c.getTipo(), c.getHorizonteHoras(),
                        c.getPercentualAfetados(), c.getEstrategia(), c.getCriadoPor().getNome(),
                        c.getCriadoEm())));
    }

    /** Recupera o resultado salvo (como calculado na época), para consulta e comparação. */
    @Transactional(readOnly = true)
    public CenarioResponse obter(Long id) {
        CenarioEmergencia c = cenarios.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cenário não encontrado."));
        try {
            CenarioResponse salvo = json.readValue(c.getResultado(), CenarioResponse.class);
            return new CenarioResponse(c.getId(), c.getNome(), c.getTipo(), c.getHorizonteHoras(),
                    c.getPercentualAfetados(), c.getEstrategia(), c.getCriadoPor().getNome(), c.getCriadoEm(),
                    salvo.cri(), salvo.recomendacoes(), salvo.avisos());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Resultado salvo do cenário está ilegível.", e);
        }
    }

    // ---- interno ----

    private CenarioResponse calcular(CenarioRequest req, Long id, String criadoPorNome,
            java.time.Instant criadoEm) {
        ParametrosDemanda parametros = ParametrosDemanda.de(req.parametros());
        CriResponse cri = criService.calcular(parametros, req.percentualAfetados(), req.horizonteHoras(),
                req.categorias());
        String chave = estrategiaPara(req.tipo());
        PriorizacaoStrategy estrategia = estrategias.get(chave);
        List<RecomendacaoResponse> recomendacoes = estrategia.analyze(cri);

        List<String> avisos = List.of(
                "Simulação de planejamento com parâmetros configuráveis — não é uma previsão de emergência real.",
                "Estratégia aplicada: " + estrategia.titulo() + " (" + chave + ").",
                "Horizonte simulado: " + req.horizonteHoras() + " h (limite do sistema: 72 h).");

        return new CenarioResponse(id, req.nome().trim(), req.tipo(), req.horizonteHoras(),
                req.percentualAfetados(), chave, criadoPorNome, criadoEm, cri, recomendacoes, avisos);
    }

    private String escrever(Object valor) {
        try {
            return json.writeValueAsString(valor);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao serializar o cenário.", e);
        }
    }
}
