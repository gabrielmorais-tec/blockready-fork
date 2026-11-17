package br.com.blockready.service;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.blockready.dto.Requests.AtualizarStatusRequest;
import br.com.blockready.dto.Responses.HistoricoSolicitacaoResponse;
import br.com.blockready.dto.Responses.PageResponse;
import br.com.blockready.dto.Responses.SolicitacaoResponse;
import br.com.blockready.exception.BusinessException;
import br.com.blockready.exception.ResourceNotFoundException;
import br.com.blockready.mapper.SolicitacaoMapper;
import br.com.blockready.model.entity.Organizacao;
import br.com.blockready.model.entity.Solicitacao;
import br.com.blockready.model.entity.SolicitacaoHistorico;
import br.com.blockready.model.entity.Usuario;
import br.com.blockready.model.enums.CategoriaRecurso;
import br.com.blockready.model.enums.Perfil;
import br.com.blockready.model.enums.Prioridade;
import br.com.blockready.model.enums.StatusSolicitacao;
import br.com.blockready.repository.OrganizacaoRepository;
import br.com.blockready.repository.RecursoRepository;
import br.com.blockready.repository.SolicitacaoHistoricoRepository;
import br.com.blockready.repository.SolicitacaoRepository;
import br.com.blockready.repository.UsuarioRepository;
import br.com.blockready.security.UsuarioPrincipal;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

/**
 * Acompanhamento de solicitações.
 * Moradores veem as próprias; organizações aprovadas veem as da categoria dos seus recursos ou as sob sua
 * responsabilidade; gestores/administradores veem todas. Endereço e nomes só aparecem para quem atende.
 */
@Service
public class SolicitacaoService {

    public record Filtro(StatusSolicitacao status, CategoriaRecurso categoria, Prioridade prioridade) { }

    private final SolicitacaoRepository solicitacoes;
    private final SolicitacaoHistoricoRepository historicos;
    private final UsuarioRepository usuarios;
    private final OrganizacaoRepository organizacoes;
    private final RecursoRepository recursos;
    private final AuditoriaService auditoria;

    public SolicitacaoService(SolicitacaoRepository solicitacoes, SolicitacaoHistoricoRepository historicos,
            UsuarioRepository usuarios, OrganizacaoRepository organizacoes, RecursoRepository recursos,
            AuditoriaService auditoria) {
        this.solicitacoes = solicitacoes;
        this.historicos = historicos;
        this.usuarios = usuarios;
        this.organizacoes = organizacoes;
        this.recursos = recursos;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public PageResponse<SolicitacaoResponse> buscar(Filtro f, int pagina, int tamanho, UsuarioPrincipal ator) {
        Specification<Solicitacao> spec = escopo(ator);
        if (f.status() != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), f.status()));
        }
        if (f.prioridade() != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("prioridade"), f.prioridade()));
        }
        if (f.categoria() != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("necessidade").get("categoria"), f.categoria()));
        }
        var pageable = PageResponse.pageable(pagina, tamanho, Sort.by(Sort.Direction.DESC, "criadoEm"));
        return PageResponse.de(solicitacoes.findAll(spec, pageable)
                .map(s -> SolicitacaoMapper.toResponse(s, podeDetalhar(s, ator))));
    }

    @Transactional(readOnly = true)
    public SolicitacaoResponse obter(Long id, UsuarioPrincipal ator) {
        Solicitacao s = carregarVisivel(id, ator);
        return SolicitacaoMapper.toResponse(s, podeDetalhar(s, ator));
    }

    @Transactional
    public SolicitacaoResponse atualizarStatus(Long id, AtualizarStatusRequest req, UsuarioPrincipal ator) {
        Solicitacao s = carregarVisivel(id, ator);
        StatusSolicitacao novo = req.status();

        switch (ator.getPerfil()) {
            case MORADOR -> {
                if (novo != StatusSolicitacao.CANCELADA) {
                    throw new AccessDeniedException("Moradores só podem cancelar as próprias solicitações.");
                }
            }
            case ORGANIZACAO -> {
                if (!organizacaoAprovada(ator)) {
                    throw new AccessDeniedException("Organização não aprovada.");
                }
                if (s.getResponsavel() != null && !podeDetalhar(s, ator)) {
                    throw new AccessDeniedException("Solicitação sob responsabilidade de outra organização.");
                }
            }
            default -> { }
        }

        Usuario autor = usuarios.getReferenceById(ator.getId());
        StatusSolicitacao anterior = s.getStatus();
        s.transitarPara(novo, autor, req.registroAtendimento());
        historicos.save(new SolicitacaoHistorico(s, anterior, novo, req.observacao(), autor));
        if (novo == StatusSolicitacao.ATENDIDA) {
            auditoria.registrar(ator.getId(), "SOLICITACAO_ATENDIDA", "Solicitacao", id, "atendimento registrado");
        }
        return SolicitacaoMapper.toResponse(s, podeDetalhar(s, ator));
    }

    @Transactional
    public SolicitacaoResponse atribuirResponsavel(Long id, Long usuarioId, UsuarioPrincipal ator) {
        Solicitacao s = solicitacoes.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitação não encontrada."));
        Usuario alvo = usuarios.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));
        if (alvo.getPerfil() == Perfil.MORADOR || !alvo.isAtivo()) {
            throw new BusinessException("O responsável deve ser um usuário ativo de organização, gestor ou administrador.");
        }
        s.atribuirResponsavel(alvo);
        historicos.save(new SolicitacaoHistorico(s, s.getStatus(), s.getStatus(),
                "Responsável atribuído: " + alvo.getNome(), usuarios.getReferenceById(ator.getId())));
        auditoria.registrar(ator.getId(), "SOLICITACAO_RESPONSAVEL_ATRIBUIDO", "Solicitacao", id,
                "responsavelId=" + usuarioId);
        return SolicitacaoMapper.toResponse(s, true);
    }

    @Transactional(readOnly = true)
    public List<HistoricoSolicitacaoResponse> historico(Long id, UsuarioPrincipal ator) {
        Solicitacao s = carregarVisivel(id, ator);
        boolean nomes = podeDetalhar(s, ator);
        return historicos.findBySolicitacaoIdOrderByCriadoEmAsc(id).stream()
                .map(h -> SolicitacaoMapper.toResponse(h, nomes)).toList();
    }

    // ---- visibilidade ----

    private Specification<Solicitacao> escopo(UsuarioPrincipal ator) {
        switch (ator.getPerfil()) {
            case GESTOR, ADMIN:
                return (root, q, cb) -> cb.conjunction();
            case ORGANIZACAO:
                if (!organizacaoAprovada(ator)) {
                    return (root, q, cb) -> cb.disjunction();
                }
                Long orgId = ator.getOrganizacaoId();
                List<CategoriaRecurso> categorias = recursos.categoriasDaOrganizacao(orgId);
                return (root, q, cb) -> {
                    Join<Solicitacao, Usuario> resp = root.join("responsavel", JoinType.LEFT);
                    Join<Usuario, Organizacao> org = resp.join("organizacao", JoinType.LEFT);
                    Predicate atribuida = cb.equal(org.get("id"), orgId);
                    if (categorias.isEmpty()) {
                        return atribuida;
                    }
                    return cb.or(atribuida, root.get("necessidade").get("categoria").in(categorias));
                };
            default:
                Long moradorId = ator.getId();
                return (root, q, cb) -> cb.equal(root.get("necessidade").get("morador").get("id"), moradorId);
        }
    }

    private boolean organizacaoAprovada(UsuarioPrincipal ator) {
        return ator.getOrganizacaoId() != null && organizacoes.findById(ator.getOrganizacaoId())
                .map(Organizacao::estaAprovada).orElse(false);
    }

    /** Quem pode ver endereço e nomes: o solicitante, gestores/administradores e a organização responsável. */
    boolean podeDetalhar(Solicitacao s, UsuarioPrincipal ator) {
        if (ator.isStaff()) {
            return true;
        }
        if (s.getNecessidade().getMorador().getId().equals(ator.getId())) {
            return true;
        }
        Usuario resp = s.getResponsavel();
        return ator.getPerfil() == Perfil.ORGANIZACAO && ator.getOrganizacaoId() != null && resp != null
                && resp.getOrganizacao() != null && ator.getOrganizacaoId().equals(resp.getOrganizacao().getId());
    }

    private boolean podeVer(Solicitacao s, UsuarioPrincipal ator) {
        if (podeDetalhar(s, ator)) {
            return true;
        }
        return ator.getPerfil() == Perfil.ORGANIZACAO && organizacaoAprovada(ator)
                && recursos.categoriasDaOrganizacao(ator.getOrganizacaoId()).contains(s.getNecessidade().getCategoria());
    }

    private Solicitacao carregarVisivel(Long id, UsuarioPrincipal ator) {
        Solicitacao s = solicitacoes.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitação não encontrada."));
        if (!podeVer(s, ator)) {
            throw new ResourceNotFoundException("Solicitação não encontrada.");
        }
        return s;
    }
}
