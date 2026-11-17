package br.com.blockready.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.blockready.dto.Responses.PageResponse;
import br.com.blockready.dto.Responses.SetorCensitarioResponse;
import br.com.blockready.dto.Responses.TerritorioResumoResponse;
import br.com.blockready.model.entity.SetorCensitario;
import br.com.blockready.repository.RegistroDesastreRepository;
import br.com.blockready.repository.RegistroMeteorologicoRepository;
import br.com.blockready.repository.SetorCensitarioRepository;

/**
 * Consulta dos dados territoriais importados: setores censitários, resumo e geometrias em GeoJSON
 * (SRID 4326, o mesmo dos recursos) para o mapa. Somente leitura; a escrita acontece na importação.
 */
@Service
public class TerritorioService {

    private static final int MAXIMO_GEOMETRIAS = 2000;

    private final SetorCensitarioRepository setores;
    private final RegistroMeteorologicoRepository meteorologicos;
    private final RegistroDesastreRepository desastres;

    public TerritorioService(SetorCensitarioRepository setores, RegistroMeteorologicoRepository meteorologicos,
            RegistroDesastreRepository desastres) {
        this.setores = setores;
        this.meteorologicos = meteorologicos;
        this.desastres = desastres;
    }

    @Transactional(readOnly = true)
    public TerritorioResumoResponse resumo() {
        return new TerritorioResumoResponse(setores.count(), setores.populacaoTotal(),
                setores.domiciliosTotal(), setores.countByGeometriaIsNotNull(),
                meteorologicos.totalEstacoes(), meteorologicos.ultimaMedicao(), desastres.count());
    }

    @Transactional(readOnly = true)
    public PageResponse<SetorCensitarioResponse> listarSetores(int pagina, int tamanho) {
        var pageable = PageResponse.pageable(pagina, tamanho, Sort.unsorted());
        return PageResponse.de(setores.findByOrderByCodigoSetorAsc(pageable).map(this::mapear));
    }

    /** FeatureCollection GeoJSON com as malhas importadas, para o Leaflet. */
    @Transactional(readOnly = true)
    public Map<String, Object> setoresGeoJson() {
        List<Map<String, Object>> features = new ArrayList<>();
        for (SetorCensitario s : setores.findAll(Sort.by("codigoSetor"))) {
            if (s.getGeometria() == null) {
                continue;
            }
            if (features.size() >= MAXIMO_GEOMETRIAS) {
                break;
            }
            Map<String, Object> propriedades = new LinkedHashMap<>();
            propriedades.put("codigoSetor", s.getCodigoSetor());
            propriedades.put("municipio", s.getMunicipio());
            propriedades.put("uf", s.getUf());
            propriedades.put("populacao", s.getPopulacao());
            propriedades.put("domicilios", s.getDomicilios());
            propriedades.put("demonstrativo", s.getImportacao().isDemonstrativo());

            Map<String, Object> feature = new LinkedHashMap<>();
            feature.put("type", "Feature");
            feature.put("properties", propriedades);
            feature.put("geometry", geometriaGeoJson(s.getGeometria()));
            features.add(feature);
        }
        Map<String, Object> colecao = new LinkedHashMap<>();
        colecao.put("type", "FeatureCollection");
        colecao.put("features", features);
        return colecao;
    }

    /** Resumo pluviométrico por estação nas últimas {@code horas} (padrão: 72h do planejamento). */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> estacoes(int horas) {
        int janela = Math.min(Math.max(horas, 1), 24 * 90);
        Instant desde = Instant.now().minus(janela, ChronoUnit.HOURS);
        List<Map<String, Object>> resposta = new ArrayList<>();
        for (Object[] linha : meteorologicos.resumoPorEstacao(desde)) {
            Map<String, Object> estacao = new LinkedHashMap<>();
            estacao.put("codigo", linha[0]);
            estacao.put("nome", linha[1]);
            estacao.put("municipio", linha[2]);
            estacao.put("uf", linha[3]);
            estacao.put("latitude", linha[4]);
            estacao.put("longitude", linha[5]);
            estacao.put("acumuladoMm", linha[6]);
            estacao.put("maximoMm", linha[7]);
            estacao.put("ultimaMedicao", linha[8]);
            resposta.add(estacao);
        }
        return resposta;
    }

    private SetorCensitarioResponse mapear(SetorCensitario s) {
        return new SetorCensitarioResponse(s.getId(), s.getCodigoSetor(), s.getMunicipio(), s.getUf(),
                s.getPopulacao(), s.getDomicilios(), s.getAreaKm2(), s.getGeometria() != null,
                s.getImportacao().isDemonstrativo(), s.getAtualizadoEm());
    }

    // ---- GeoJSON (MultiPolygon, ordem lon/lat conforme a especificação) ----

    private Map<String, Object> geometriaGeoJson(MultiPolygon mp) {
        List<Object> poligonos = new ArrayList<>();
        for (int i = 0; i < mp.getNumGeometries(); i++) {
            poligonos.add(poligono((Polygon) mp.getGeometryN(i)));
        }
        Map<String, Object> g = new LinkedHashMap<>();
        g.put("type", "MultiPolygon");
        g.put("coordinates", poligonos);
        return g;
    }

    private List<Object> poligono(Polygon p) {
        List<Object> aneis = new ArrayList<>();
        aneis.add(anel(p.getExteriorRing().getCoordinates()));
        for (int i = 0; i < p.getNumInteriorRing(); i++) {
            aneis.add(anel(p.getInteriorRingN(i).getCoordinates()));
        }
        return aneis;
    }

    private List<double[]> anel(Coordinate[] coordenadas) {
        List<double[]> pontos = new ArrayList<>(coordenadas.length);
        for (Coordinate c : coordenadas) {
            pontos.add(new double[] { arredondar(c.x), arredondar(c.y) });
        }
        return pontos;
    }

    private double arredondar(double v) {
        return BigDecimal.valueOf(v).setScale(6, java.math.RoundingMode.HALF_UP).doubleValue();
    }
}
