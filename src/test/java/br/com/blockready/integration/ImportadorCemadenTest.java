package br.com.blockready.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import br.com.blockready.exception.BusinessException;
import br.com.blockready.integration.cemaden.ImportadorCemaden;
import br.com.blockready.model.entity.RegistroMeteorologico;
import br.com.blockready.repository.RegistroMeteorologicoRepository;

class ImportadorCemadenTest {

    private static final String CABECALHO = "codEstacao;nomeEstacao;municipio;uf;latitude;longitude;datahora;valorMedida\n";

    private RegistroMeteorologicoRepository repo;
    private ImportadorCemaden importador;

    @BeforeEach
    void preparar() {
        repo = mock(RegistroMeteorologicoRepository.class);
        when(repo.existsByEstacaoCodigoAndMedidoEm(any(), any())).thenReturn(false);
        importador = new ImportadorCemaden(repo);
    }

    private CsvArquivo csv(String conteudo) {
        return CsvArquivo.ler(conteudo.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void importaMedicaoValidaConvertendoHorarioParaUtc() {
        CsvArquivo arquivo = csv(CABECALHO
                + "PR001;Centro;Curitiba;PR;-25,43;-49,27;2026-11-10 12:00:00;15,5\n");

        var resultado = importador.importar(arquivo, null);

        assertEquals(1, resultado.processados());
        assertEquals(0, resultado.rejeitados());
        ArgumentCaptor<RegistroMeteorologico> captor = ArgumentCaptor.forClass(RegistroMeteorologico.class);
        verify(repo).save(captor.capture());
        assertEquals(Instant.parse("2026-11-10T12:00:00Z"), captor.getValue().getMedidoEm());
        assertEquals("PR001", captor.getValue().getEstacaoCodigo());
    }

    @Test
    void rejeitaValorAusenteNegativoOuDataInvalida() {
        CsvArquivo arquivo = csv(CABECALHO
                + "PR001;Centro;Curitiba;PR;-25,43;-49,27;2026-11-10 12:00:00;\n"
                + "PR001;Centro;Curitiba;PR;-25,43;-49,27;2026-11-10 13:00:00;-3\n"
                + "PR001;Centro;Curitiba;PR;-25,43;-49,27;hoje cedo;10\n");

        var resultado = importador.importar(arquivo, null);

        assertEquals(0, resultado.processados());
        assertEquals(3, resultado.rejeitados());
        verify(repo, times(0)).save(any());
    }

    @Test
    void controlaDuplicidadeDentroDoArquivoEEntreImportacoes() {
        when(repo.existsByEstacaoCodigoAndMedidoEm("PR009", Instant.parse("2026-11-10T08:00:00Z")))
                .thenReturn(true);
        CsvArquivo arquivo = csv(CABECALHO
                + "PR001;Centro;Curitiba;PR;-25,43;-49,27;2026-11-10 12:00:00;5\n"
                + "PR001;Centro;Curitiba;PR;-25,43;-49,27;2026-11-10 12:00:00;5\n"
                + "PR009;Sul;Curitiba;PR;-25,50;-49,30;2026-11-10 08:00:00;7\n");

        var resultado = importador.importar(arquivo, null);

        assertEquals(1, resultado.processados());
        assertEquals(2, resultado.rejeitados());
        assertTrue(resultado.erros().get(0).contains("duplicada no arquivo"));
        assertTrue(resultado.erros().get(1).contains("já importada"));
    }

    @Test
    void cabecalhoIncompativelEhRejeitadoAntesDeProcessar() {
        assertThrows(BusinessException.class,
                () -> importador.validarCabecalho(csv("a;b;c\n1;2;3\n")));
    }
}
