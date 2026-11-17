package br.com.blockready.service;

import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.blockready.dto.Requests.OrganizacaoRequest;
import br.com.blockready.dto.Responses.OrganizacaoResponse;
import br.com.blockready.dto.Responses.PageResponse;
import br.com.blockready.exception.ResourceNotFoundException;
import br.com.blockready.mapper.OrganizacaoMapper;
import br.com.blockready.model.entity.Organizacao;
import br.com.blockready.model.enums.StatusAprovacao;
import br.com.blockready.repository.OrganizacaoRepository;
import br.com.blockready.security.UsuarioPrincipal;

@Service
public class OrganizacaoService {

    private final OrganizacaoRepository organizacoes;
    private final AuditoriaService auditoria;

    public OrganizacaoService(OrganizacaoRepository organizacoes, AuditoriaService auditoria) {
        this.organizacoes = organizacoes;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public PageResponse<OrganizacaoResponse> listar(StatusAprovacao status, int pagina, int tamanho) {
        var pageable = PageResponse.pageable(pagina, tamanho, Sort.by(Sort.Direction.DESC, "criadoEm"));
        var page = status == null ? organizacoes.findAll(pageable) : organizacoes.findByStatusAprovacao(status, pageable);
        return PageResponse.de(page.map(OrganizacaoMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public OrganizacaoResponse obter(Long id, UsuarioPrincipal ator) {
        Organizacao o = carregar(id);
        exigirAcesso(o, ator);
        return OrganizacaoMapper.toResponse(o);
    }

    @Transactional(readOnly = true)
    public OrganizacaoResponse minha(UsuarioPrincipal ator) {
        if (ator.getOrganizacaoId() == null) {
            throw new ResourceNotFoundException("Você não está vinculado a uma organização.");
        }
        return OrganizacaoMapper.toResponse(carregar(ator.getOrganizacaoId()));
    }

    @Transactional
    public OrganizacaoResponse atualizar(Long id, OrganizacaoRequest r, UsuarioPrincipal ator) {
        Organizacao o = carregar(id);
        exigirAcesso(o, ator);
        OrganizacaoMapper.aplicar(o, r);
        auditoria.registrar(ator.getId(), "ORGANIZACAO_ATUALIZADA", "Organizacao", id, null);
        return OrganizacaoMapper.toResponse(o);
    }

    @Transactional
    public OrganizacaoResponse decidir(Long id, StatusAprovacao decisao, UsuarioPrincipal ator) {
        Organizacao o = carregar(id);
        switch (decisao) {
            case APROVADA -> o.aprovar();
            case REJEITADA -> o.rejeitar();
            case SUSPENSA -> o.suspender();
            default -> throw new IllegalArgumentException("Decisão inválida: " + decisao);
        }
        auditoria.registrar(ator.getId(), "ORGANIZACAO_" + decisao, "Organizacao", id, o.getNome());
        return OrganizacaoMapper.toResponse(o);
    }

    private void exigirAcesso(Organizacao o, UsuarioPrincipal ator) {
        boolean dona = ator.getOrganizacaoId() != null && ator.getOrganizacaoId().equals(o.getId());
        if (!ator.isStaff() && !dona) {
            throw new AccessDeniedException("Sem permissão sobre esta organização.");
        }
    }

    private Organizacao carregar(Long id) {
        return organizacoes.findById(id).orElseThrow(() -> new ResourceNotFoundException("Organização não encontrada."));
    }
}
