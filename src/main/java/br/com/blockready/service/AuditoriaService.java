package br.com.blockready.service;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.blockready.dto.Responses.AuditoriaResponse;
import br.com.blockready.dto.Responses.PageResponse;
import br.com.blockready.model.entity.AuditoriaRegistro;
import br.com.blockready.repository.AuditoriaRepository;

/** Registro de operações sensíveis. Participa da transação de quem chama. */
@Service
public class AuditoriaService {

    private final AuditoriaRepository repository;

    public AuditoriaService(AuditoriaRepository repository) { this.repository = repository; }

    @Transactional
    public void registrar(Long usuarioId, String acao, String entidade, Long entidadeId, String detalhes) {
        String d = detalhes != null && detalhes.length() > 1000 ? detalhes.substring(0, 1000) : detalhes;
        repository.save(new AuditoriaRegistro(usuarioId, acao, entidade, entidadeId, d));
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditoriaResponse> listar(int pagina, int tamanho) {
        return PageResponse.de(repository
                .findAll(PageResponse.pageable(pagina, tamanho, Sort.by(Sort.Direction.DESC, "criadoEm")))
                .map(a -> new AuditoriaResponse(a.getId(), a.getUsuarioId(), a.getAcao(), a.getEntidade(),
                        a.getEntidadeId(), a.getDetalhes(), a.getCriadoEm())));
    }
}
