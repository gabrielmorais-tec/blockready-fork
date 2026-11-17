package br.com.blockready.service;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import br.com.blockready.dto.Responses.FonteExternaResponse;
import br.com.blockready.dto.Responses.ImportacaoResponse;
import br.com.blockready.dto.Responses.PageResponse;
import br.com.blockready.dto.Responses.PreviewImportacaoResponse;
import br.com.blockready.exception.BusinessException;
import br.com.blockready.exception.ConflictException;
import br.com.blockready.exception.ResourceNotFoundException;
import br.com.blockready.integration.CsvArquivo;
import br.com.blockready.integration.ImportadorDataset;
import br.com.blockready.model.entity.FonteExterna;
import br.com.blockready.model.entity.ImportacaoDataset;
import br.com.blockready.model.enums.StatusImportacao;
import br.com.blockready.repository.FonteExternaRepository;
import br.com.blockready.repository.ImportacaoDatasetRepository;
import br.com.blockready.repository.RegistroDesastreRepository;
import br.com.blockready.repository.RegistroMeteorologicoRepository;
import br.com.blockready.repository.SetorCensitarioRepository;
import br.com.blockready.repository.UsuarioRepository;
import br.com.blockready.security.UsuarioPrincipal;

/**
 * Orquestra as importações de datasets públicos: resolve a fonte e o importador, valida o arquivo,
 * controla duplicidade por hash, executa o processamento e registra o histórico da operação.
 * A lógica de cada fonte vive nos importadores de {@code integration}, fora dos controllers.
 */
@Service
public class ImportacaoService {

    private static final long TAMANHO_MAXIMO_BYTES = 20L * 1024 * 1024;
    private static final int MAXIMO_ERROS_REGISTRADOS = 50;
    private static final int LINHAS_PREVIA = 8;

    private final Map<String, ImportadorDataset> importadores;
    private final FonteExternaRepository fontes;
    private final ImportacaoDatasetRepository importacoes;
    private final SetorCensitarioRepository setores;
    private final RegistroMeteorologicoRepository meteorologicos;
    private final RegistroDesastreRepository desastres;
    private final UsuarioRepository usuarios;
    private final AuditoriaService auditoria;

    public ImportacaoService(List<ImportadorDataset> listaImportadores, FonteExternaRepository fontes,
            ImportacaoDatasetRepository importacoes, SetorCensitarioRepository setores,
            RegistroMeteorologicoRepository meteorologicos, RegistroDesastreRepository desastres,
            UsuarioRepository usuarios, AuditoriaService auditoria) {
        this.importadores = listaImportadores.stream()
                .collect(Collectors.toMap(ImportadorDataset::fonteCodigo, Function.identity()));
        this.fontes = fontes;
        this.importacoes = importacoes;
        this.setores = setores;
        this.meteorologicos = meteorologicos;
        this.desastres = desastres;
        this.usuarios = usuarios;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<FonteExternaResponse> listarFontes() {
        return fontes.findAll(Sort.by("id")).stream().map(f -> {
            long registros = switch (f.getCodigo()) {
                case "IBGE_CENSO" -> setores.count();
                case "CEMADEN_PLUVIO" -> meteorologicos.count();
                case "ATLAS_DESASTRES" -> desastres.count();
                default -> 0;
            };
            ImportadorDataset imp = importadores.get(f.getCodigo());
            return new FonteExternaResponse(f.getId(), f.getCodigo(), f.getNome(), f.getDescricao(),
                    f.getUrlBase(), f.getAbrangencia(), f.getUltimaImportacao(), registros,
                    imp != null ? imp.colunasEsperadas() : null);
        }).toList();
    }

    /** Pré-visualização: valida o cabeçalho e devolve as primeiras linhas, sem gravar nada. */
    public PreviewImportacaoResponse previa(String fonteCodigo, MultipartFile arquivo) {
        ImportadorDataset importador = importador(fonteCodigo);
        CsvArquivo csv = lerCsv(arquivo);
        importador.validarCabecalho(csv);
        List<Map<String, String>> linhas = new ArrayList<>();
        for (CsvArquivo.Linha l : csv.linhas().subList(0, Math.min(LINHAS_PREVIA, csv.linhas().size()))) {
            linhas.add(new LinkedHashMap<>(l.valores()));
        }
        return new PreviewImportacaoResponse(csv.cabecalho(), linhas, csv.linhas().size(),
                String.valueOf(csv.delimitador()));
    }

    @Transactional
    public ImportacaoResponse importar(String fonteCodigo, MultipartFile arquivo, String periodoReferencia,
            boolean demonstrativo, UsuarioPrincipal ator) {
        ImportadorDataset importador = importador(fonteCodigo);
        FonteExterna fonte = fontes.findByCodigo(fonteCodigo)
                .orElseThrow(() -> new ResourceNotFoundException("Fonte externa não encontrada."));

        byte[] conteudo = bytes(arquivo);
        String hash = sha256(conteudo);
        if (importacoes.existsByFonteIdAndHashArquivoAndStatusNot(fonte.getId(), hash, StatusImportacao.FALHA)) {
            throw new ConflictException("Este arquivo já foi importado para esta fonte (duplicidade por conteúdo).");
        }

        CsvArquivo csv = CsvArquivo.ler(conteudo);
        importador.validarCabecalho(csv);

        // O registro é salvo antes do processamento: as entidades importadas referenciam esta importação.
        ImportacaoDataset registro = importacoes.save(new ImportacaoDataset(fonte, nomeArquivo(arquivo), hash,
                periodoReferencia, demonstrativo, usuarios.getReferenceById(ator.getId())));
        ImportadorDataset.Resultado resultado = importador.importar(csv, registro);
        registro.concluir(resultado.processados(), resultado.rejeitados(), resumirErros(resultado.erros()));
        fonte.registrarImportacao();

        auditoria.registrar(ator.getId(), "DATASET_IMPORTADO", "ImportacaoDataset", registro.getId(),
                fonte.getCodigo() + ": " + registro.getNomeArquivo() + " (" + resultado.processados()
                        + " processados, " + resultado.rejeitados() + " rejeitados)");
        return mapear(registro);
    }

    @Transactional(readOnly = true)
    public PageResponse<ImportacaoResponse> historico(Long fonteId, int pagina, int tamanho) {
        var pageable = PageResponse.pageable(pagina, tamanho, Sort.unsorted());
        var page = fonteId != null
                ? importacoes.findByFonteIdOrderByCriadoEmDesc(fonteId, pageable)
                : importacoes.findByOrderByCriadoEmDesc(pageable);
        return PageResponse.de(page.map(this::mapear));
    }

    @Transactional(readOnly = true)
    public ImportacaoResponse obter(Long id) {
        return importacoes.findById(id).map(this::mapear)
                .orElseThrow(() -> new ResourceNotFoundException("Importação não encontrada."));
    }

    // ---- apoio ----

    private ImportadorDataset importador(String fonteCodigo) {
        ImportadorDataset imp = fonteCodigo != null ? importadores.get(fonteCodigo) : null;
        if (imp == null) {
            throw new BusinessException("Selecione uma fonte válida: " + String.join(", ", importadores.keySet()) + ".");
        }
        return imp;
    }

    private CsvArquivo lerCsv(MultipartFile arquivo) {
        return CsvArquivo.ler(bytes(arquivo));
    }

    private byte[] bytes(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new BusinessException("Envie um arquivo CSV.");
        }
        if (arquivo.getSize() > TAMANHO_MAXIMO_BYTES) {
            throw new BusinessException("O arquivo excede o limite de 20 MB.");
        }
        try {
            return arquivo.getBytes();
        } catch (IOException e) {
            throw new BusinessException("Não foi possível ler o arquivo enviado.");
        }
    }

    private String nomeArquivo(MultipartFile arquivo) {
        String nome = arquivo.getOriginalFilename();
        if (nome == null || nome.isBlank()) {
            return "arquivo.csv";
        }
        nome = nome.replace("\\", "/");
        nome = nome.substring(nome.lastIndexOf('/') + 1);
        return nome.length() > 260 ? nome.substring(nome.length() - 260) : nome;
    }

    private String resumirErros(List<String> erros) {
        if (erros.isEmpty()) {
            return null;
        }
        List<String> primeiros = erros.subList(0, Math.min(MAXIMO_ERROS_REGISTRADOS, erros.size()));
        String resumo = String.join("\n", primeiros);
        if (erros.size() > primeiros.size()) {
            resumo += "\n… e mais " + (erros.size() - primeiros.size()) + " erro(s).";
        }
        return resumo;
    }

    private ImportacaoResponse mapear(ImportacaoDataset i) {
        return new ImportacaoResponse(i.getId(), i.getFonte().getCodigo(), i.getFonte().getNome(),
                i.getNomeArquivo(), i.getPeriodoReferencia(), i.isDemonstrativo(), i.getStatus(),
                i.getTotalProcessados(), i.getTotalRejeitados(), i.getErros(),
                i.getImportadoPor().getNome(), i.getCriadoEm());
    }

    private static String sha256(byte[] conteudo) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(conteudo));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível.", e);
        }
    }
}
