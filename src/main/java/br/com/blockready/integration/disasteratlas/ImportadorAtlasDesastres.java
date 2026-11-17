package br.com.blockready.integration.disasteratlas;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import br.com.blockready.exception.BusinessException;
import br.com.blockready.integration.Campos;
import br.com.blockready.integration.CsvArquivo;
import br.com.blockready.integration.ImportadorDataset;
import br.com.blockready.model.entity.ImportacaoDataset;
import br.com.blockready.model.entity.RegistroDesastre;
import br.com.blockready.repository.RegistroDesastreRepository;

/**
 * Importa o histórico de ocorrências do Atlas Digital de Desastres (MIDR).
 * Colunas no padrão das planilhas públicas do Atlas (município, UF, tipologia/COBRADE, data,
 * danos humanos e prejuízos), com apelidos aceitos.
 *
 * <p>A abrangência desses registros é MUNICIPAL: o sistema não os atribui a bairros ou
 * setores específicos. Duplicidade controlada por município + tipo + data.</p>
 */
@Component
public class ImportadorAtlasDesastres implements ImportadorDataset {

    private static final String[] COL_MUNICIPIO = { "municipio", "nome_municipio", "nm_mun" };
    private static final String[] COL_UF = { "uf", "sigla_uf" };
    private static final String[] COL_TIPO = { "tipo_desastre", "descricao_tipologia", "tipologia", "grupo_de_desastre", "tipo" };
    private static final String[] COL_COBRADE = { "cobrade", "cod_cobrade" };
    private static final String[] COL_DATA = { "data_registro", "data_ocorrencia", "data", "ocorrido_em" };
    private static final String[] COL_MORTOS = { "dh_mortos", "mortos", "obitos" };
    private static final String[] COL_AFETADOS = { "dh_total_afetados", "afetados", "pessoas_afetadas" };
    private static final String[] COL_DESABRIGADOS = { "dh_desabrigados", "desabrigados" };
    private static final String[] COL_DESALOJADOS = { "dh_desalojados", "desalojados" };
    private static final String[] COL_PREJUIZO = { "prejuizo_total", "pepl_total", "pepr_total", "prejuizo" };

    private final RegistroDesastreRepository registros;

    public ImportadorAtlasDesastres(RegistroDesastreRepository registros) { this.registros = registros; }

    @Override
    public String fonteCodigo() { return "ATLAS_DESASTRES"; }

    @Override
    public String colunasEsperadas() {
        return "municipio; uf; tipo_desastre (ou descricao_tipologia); cobrade (opcional); data_registro; "
                + "DH_MORTOS; DH_TOTAL_AFETADOS; DH_DESABRIGADOS; DH_DESALOJADOS; prejuizo_total (opcional)";
    }

    @Override
    public void validarCabecalho(CsvArquivo csv) {
        if (!csv.possuiColuna(COL_MUNICIPIO) || !csv.possuiColuna(COL_TIPO) || !csv.possuiColuna(COL_DATA)) {
            throw new BusinessException("Arquivo incompatível com o Atlas de Desastres: esperadas as colunas "
                    + colunasEsperadas() + ".");
        }
    }

    @Override
    public Resultado importar(CsvArquivo csv, ImportacaoDataset importacao) {
        int processados = 0;
        List<String> erros = new ArrayList<>();
        Set<String> vistosNoArquivo = new HashSet<>();

        for (CsvArquivo.Linha linha : csv.linhas()) {
            String municipio = linha.texto(COL_MUNICIPIO);
            String uf = linha.texto(COL_UF);
            String tipo = linha.texto(COL_TIPO);
            LocalDate data = Campos.data(linha.texto(COL_DATA));

            if (municipio == null || uf == null || uf.length() != 2) {
                erros.add("Linha " + linha.numero() + ": município/UF ausentes ou inválidos.");
                continue;
            }
            if (tipo == null || tipo.length() > 160) {
                erros.add("Linha " + linha.numero() + ": tipo de desastre ausente ou inválido.");
                continue;
            }
            if (data == null) {
                erros.add("Linha " + linha.numero() + ": data da ocorrência ausente ou em formato não reconhecido.");
                continue;
            }
            Integer mortos = Campos.inteiro(linha.texto(COL_MORTOS));
            Integer afetados = Campos.inteiro(linha.texto(COL_AFETADOS));
            Integer desabrigados = Campos.inteiro(linha.texto(COL_DESABRIGADOS));
            Integer desalojados = Campos.inteiro(linha.texto(COL_DESALOJADOS));
            if (negativo(mortos) || negativo(afetados) || negativo(desabrigados) || negativo(desalojados)) {
                erros.add("Linha " + linha.numero() + ": danos humanos não podem ser negativos.");
                continue;
            }
            BigDecimal prejuizo = Campos.decimal(linha.texto(COL_PREJUIZO));
            if (prejuizo != null && prejuizo.signum() < 0) {
                erros.add("Linha " + linha.numero() + ": prejuízo não pode ser negativo.");
                continue;
            }
            String chave = (municipio + "|" + uf + "|" + tipo + "|" + data).toLowerCase();
            if (!vistosNoArquivo.add(chave)) {
                erros.add("Linha " + linha.numero() + ": ocorrência duplicada no arquivo.");
                continue;
            }
            if (registros.existsByMunicipioAndUfAndTipoDesastreAndOcorridoEm(municipio, uf.toUpperCase(), tipo, data)) {
                erros.add("Linha " + linha.numero() + ": ocorrência já importada anteriormente.");
                continue;
            }
            registros.save(new RegistroDesastre(municipio, uf.toUpperCase(), tipo, linha.texto(COL_COBRADE),
                    data, Campos.zeroSeNulo(mortos), Campos.zeroSeNulo(afetados), Campos.zeroSeNulo(desabrigados),
                    Campos.zeroSeNulo(desalojados), prejuizo, importacao));
            processados++;
        }
        return new Resultado(processados, erros.size(), erros);
    }

    private boolean negativo(Integer v) { return v != null && v < 0; }
}
