package br.com.blockready.integration.cemaden;

import java.math.BigDecimal;
import java.time.Instant;
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
import br.com.blockready.model.entity.RegistroMeteorologico;
import br.com.blockready.repository.RegistroMeteorologicoRepository;

/**
 * Importa medições pluviométricas dos arquivos públicos do CEMADEN.
 * Colunas no padrão dos downloads do CEMADEN (codEstacao, nomeEstacao, municipio, uf,
 * latitude, longitude, datahora, valorMedida), com apelidos aceitos.
 *
 * <p>Tratamento aplicado: horários convertidos/armazenados em UTC, valores ausentes ou
 * negativos rejeitados com o motivo, estação de origem identificada em cada registro e
 * duplicidade controlada por estação + instante da medição.</p>
 */
@Component
public class ImportadorCemaden implements ImportadorDataset {

    private static final String[] COL_ESTACAO = { "codestacao", "cod_estacao", "codigo_estacao", "estacao" };
    private static final String[] COL_NOME = { "nomeestacao", "nome_estacao" };
    private static final String[] COL_MUNICIPIO = { "municipio", "cidade" };
    private static final String[] COL_UF = { "uf", "sigla_uf" };
    private static final String[] COL_LAT = { "latitude", "lat" };
    private static final String[] COL_LON = { "longitude", "lon", "lng" };
    private static final String[] COL_DATA = { "datahora", "data_hora", "data_hora_utc", "datahora_utc" };
    private static final String[] COL_VALOR = { "valormedida", "valor_medida", "precipitacao_mm", "chuva_mm", "precipitacao" };

    private final RegistroMeteorologicoRepository registros;

    public ImportadorCemaden(RegistroMeteorologicoRepository registros) { this.registros = registros; }

    @Override
    public String fonteCodigo() { return "CEMADEN_PLUVIO"; }

    @Override
    public String colunasEsperadas() {
        return "codEstacao; nomeEstacao; municipio; uf; latitude; longitude; "
                + "datahora (UTC); valorMedida (mm)";
    }

    @Override
    public void validarCabecalho(CsvArquivo csv) {
        if (!csv.possuiColuna(COL_ESTACAO) || !csv.possuiColuna(COL_DATA) || !csv.possuiColuna(COL_VALOR)) {
            throw new BusinessException("Arquivo incompatível com o CEMADEN: esperadas as colunas "
                    + colunasEsperadas() + ".");
        }
    }

    @Override
    public Resultado importar(CsvArquivo csv, ImportacaoDataset importacao) {
        int processados = 0;
        List<String> erros = new ArrayList<>();
        Set<String> vistosNoArquivo = new HashSet<>();

        for (CsvArquivo.Linha linha : csv.linhas()) {
            String estacao = linha.texto(COL_ESTACAO);
            Instant medidoEm = Campos.instanteUtc(linha.texto(COL_DATA));
            BigDecimal valor = Campos.decimal(linha.texto(COL_VALOR));
            Double lat = Campos.coordenada(linha.texto(COL_LAT), -90, 90);
            Double lon = Campos.coordenada(linha.texto(COL_LON), -180, 180);
            String municipio = linha.texto(COL_MUNICIPIO);
            String uf = linha.texto(COL_UF);

            if (estacao == null || estacao.length() > 40) {
                erros.add("Linha " + linha.numero() + ": estação de origem não identificada.");
                continue;
            }
            if (medidoEm == null) {
                erros.add("Linha " + linha.numero() + ": data/hora ausente ou em formato não reconhecido.");
                continue;
            }
            if (valor == null || valor.signum() < 0) {
                erros.add("Linha " + linha.numero() + ": precipitação ausente, inválida ou negativa.");
                continue;
            }
            if (lat == null || lon == null) {
                erros.add("Linha " + linha.numero() + ": coordenadas da estação ausentes ou fora do intervalo.");
                continue;
            }
            if (municipio == null || uf == null || uf.length() != 2) {
                erros.add("Linha " + linha.numero() + ": município/UF da estação ausentes ou inválidos.");
                continue;
            }
            String chave = estacao + "|" + medidoEm;
            if (!vistosNoArquivo.add(chave)) {
                erros.add("Linha " + linha.numero() + ": medição duplicada no arquivo (" + estacao + ").");
                continue;
            }
            if (registros.existsByEstacaoCodigoAndMedidoEm(estacao, medidoEm)) {
                erros.add("Linha " + linha.numero() + ": medição já importada anteriormente (" + estacao + ").");
                continue;
            }
            registros.save(new RegistroMeteorologico(estacao, linha.texto(COL_NOME), municipio,
                    uf.toUpperCase(), lat, lon, medidoEm, valor, importacao));
            processados++;
        }
        return new Resultado(processados, erros.size(), erros);
    }
}
