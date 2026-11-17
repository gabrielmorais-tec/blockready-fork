package br.com.blockready.integration.ibge;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.WKTReader;
import org.springframework.stereotype.Component;

import br.com.blockready.exception.BusinessException;
import br.com.blockready.integration.Campos;
import br.com.blockready.integration.CsvArquivo;
import br.com.blockready.integration.ImportadorDataset;
import br.com.blockready.model.entity.ImportacaoDataset;
import br.com.blockready.model.entity.SetorCensitario;
import br.com.blockready.repository.SetorCensitarioRepository;

/**
 * Importa os agregados por setor censitário do Censo Demográfico 2022 (IBGE).
 * Colunas seguem os nomes dos arquivos públicos de agregados (CD_SETOR, NM_MUN, SIGLA_UF,
 * v0001 = população, v0002 = domicílios), com apelidos alternativos aceitos.
 * A malha é opcional, em WKT (SRID 4326), no padrão da malha de setores distribuída pelo IBGE.
 */
@Component
public class ImportadorIbgeCenso implements ImportadorDataset {

    private static final String[] COL_SETOR = { "cd_setor", "codigo_setor", "setor" };
    private static final String[] COL_MUNICIPIO = { "nm_mun", "municipio", "nome_municipio" };
    private static final String[] COL_UF = { "sigla_uf", "uf" };
    private static final String[] COL_POPULACAO = { "v0001", "populacao", "pop_total" };
    private static final String[] COL_DOMICILIOS = { "v0002", "domicilios", "total_domicilios" };
    private static final String[] COL_AREA = { "area_km2", "areakm2" };
    private static final String[] COL_GEOMETRIA = { "wkt", "geometria", "geometry", "geom" };

    private final SetorCensitarioRepository setores;
    private final GeometryFactory geometrias = new GeometryFactory(new PrecisionModel(), 4326);

    public ImportadorIbgeCenso(SetorCensitarioRepository setores) { this.setores = setores; }

    @Override
    public String fonteCodigo() { return "IBGE_CENSO"; }

    @Override
    public String colunasEsperadas() {
        return "CD_SETOR; NM_MUN; SIGLA_UF; v0001 (população); v0002 (domicílios); "
                + "AREA_KM2 (opcional); WKT (malha opcional, SRID 4326)";
    }

    @Override
    public void validarCabecalho(CsvArquivo csv) {
        if (!csv.possuiColuna(COL_SETOR) || !csv.possuiColuna(COL_POPULACAO)) {
            throw new BusinessException("Arquivo incompatível com o IBGE: esperadas as colunas "
                    + colunasEsperadas() + ".");
        }
    }

    @Override
    public Resultado importar(CsvArquivo csv, ImportacaoDataset importacao) {
        int processados = 0;
        List<String> erros = new ArrayList<>();
        Set<String> vistosNoArquivo = new HashSet<>();
        WKTReader leitorWkt = new WKTReader(geometrias);

        for (CsvArquivo.Linha linha : csv.linhas()) {
            String codigo = linha.texto(COL_SETOR);
            Integer populacao = Campos.inteiro(linha.texto(COL_POPULACAO));
            Integer domicilios = Campos.inteiro(linha.texto(COL_DOMICILIOS));
            String municipio = linha.texto(COL_MUNICIPIO);
            String uf = linha.texto(COL_UF);

            if (codigo == null || codigo.length() > 25) {
                erros.add("Linha " + linha.numero() + ": código do setor ausente ou inválido.");
                continue;
            }
            if (!vistosNoArquivo.add(codigo)) {
                erros.add("Linha " + linha.numero() + ": setor " + codigo + " duplicado no arquivo.");
                continue;
            }
            if (populacao == null || populacao < 0) {
                erros.add("Linha " + linha.numero() + ": população ausente ou inválida.");
                continue;
            }
            if (municipio == null || uf == null || uf.length() != 2) {
                erros.add("Linha " + linha.numero() + ": município/UF ausentes ou inválidos.");
                continue;
            }
            MultiPolygon geometria = null;
            String wkt = linha.texto(COL_GEOMETRIA);
            if (wkt != null) {
                try {
                    Geometry g = leitorWkt.read(wkt);
                    geometria = comoMultiPolygon(g);
                } catch (Exception e) {
                    erros.add("Linha " + linha.numero() + ": geometria WKT inválida (" + e.getMessage() + ").");
                    continue;
                }
            }
            BigDecimal area = Campos.decimal(linha.texto(COL_AREA));
            int dom = Campos.zeroSeNulo(domicilios);
            MultiPolygon geo = geometria;

            // Duplicidade entre importações: reimportar um setor existente atualiza os valores.
            setores.findByCodigoSetor(codigo).ifPresentOrElse(
                    existente -> existente.atualizar(municipio, uf.toUpperCase(), populacao, dom, area, geo,
                            importacao),
                    () -> setores.save(new SetorCensitario(codigo, municipio, uf.toUpperCase(), populacao, dom,
                            area, geo, importacao)));
            processados++;
        }
        return new Resultado(processados, erros.size(), erros);
    }

    private MultiPolygon comoMultiPolygon(Geometry g) {
        if (g instanceof MultiPolygon mp) {
            mp.setSRID(4326);
            return mp;
        }
        if (g instanceof Polygon p) {
            MultiPolygon mp = geometrias.createMultiPolygon(new Polygon[] { p });
            mp.setSRID(4326);
            return mp;
        }
        throw new BusinessException("A geometria do setor deve ser Polygon ou MultiPolygon.");
    }
}
