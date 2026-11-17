package br.com.blockready.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import br.com.blockready.dto.Responses.FonteExternaResponse;
import br.com.blockready.dto.Responses.ImportacaoResponse;
import br.com.blockready.dto.Responses.PageResponse;
import br.com.blockready.dto.Responses.PreviewImportacaoResponse;
import br.com.blockready.security.UsuarioPrincipal;
import br.com.blockready.service.ImportacaoService;

/** Funcionalidade administrativa de integração de dados: fontes, pré-visualização, importação e histórico. */
@RestController
@RequestMapping("/api/v1")
@PreAuthorize("hasAnyRole('GESTOR','ADMIN')")
public class ImportacaoController {

    private final ImportacaoService service;

    public ImportacaoController(ImportacaoService service) { this.service = service; }

    @GetMapping("/fontes")
    public List<FonteExternaResponse> fontes() {
        return service.listarFontes();
    }

    @PostMapping("/importacoes/previa")
    public PreviewImportacaoResponse previa(@RequestParam String fonte, @RequestParam MultipartFile arquivo) {
        return service.previa(fonte, arquivo);
    }

    @PostMapping("/importacoes")
    @ResponseStatus(HttpStatus.CREATED)
    public ImportacaoResponse importar(@RequestParam String fonte, @RequestParam MultipartFile arquivo,
            @RequestParam(required = false) String periodoReferencia,
            @RequestParam(defaultValue = "false") boolean demonstrativo,
            @AuthenticationPrincipal UsuarioPrincipal ator) {
        return service.importar(fonte, arquivo, periodoReferencia, demonstrativo, ator);
    }

    @GetMapping("/importacoes")
    public PageResponse<ImportacaoResponse> historico(@RequestParam(required = false) Long fonteId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return service.historico(fonteId, page, size);
    }

    @GetMapping("/importacoes/{id}")
    public ImportacaoResponse obter(@PathVariable Long id) {
        return service.obter(id);
    }
}
