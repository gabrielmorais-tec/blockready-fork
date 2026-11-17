package br.com.blockready.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.blockready.dto.Requests.DisponibilidadeRequest;
import br.com.blockready.dto.Requests.RecursoRequest;
import br.com.blockready.dto.Responses.DisponibilidadeResponse;
import br.com.blockready.dto.Responses.HistoricoRecursoResponse;
import br.com.blockready.dto.Responses.PageResponse;
import br.com.blockready.dto.Responses.RecursoResponse;
import br.com.blockready.exception.BusinessException;
import br.com.blockready.exception.ResourceNotFoundException;
import br.com.blockready.mapper.RecursoMapper;
import br.com.blockready.model.entity.Abrigo;
import br.com.blockready.model.entity.Organizacao;
import br.com.blockready.model.entity.PontoAgua;
import br.com.blockready.model.entity.PontoAlimentos;
import br.com.blockready.model.entity.PontoEnergia;
import br.com.blockready.model.entity.RecursoComunitario;
import br.com.blockready.model.entity.RecursoHistorico;
import br.com.blockready.model.entity.RegistroDisponibilidade;
import br.com.blockready.model.entity.Usuario;
import br.com.blockready.model.enums.CategoriaRecurso;
import br.com.blockready.model.enums.Perfil;
import br.com.blockready.model.enums.StatusRecurso;
import br.com.blockready.repository.OrganizacaoRepository;
import br.com.blockready.repository.RecursoHistoricoRepository;
import br.com.blockready.repository.RecursoRepository;
import br.com.blockready.repository.RegistroDisponibilidadeRepository;
import br.com.blockready.repository.UsuarioRepository;
import br.com.blockready.security.UsuarioPrincipal;

/**
 * Regras do módulo de recursos comunitários.
 * Visibilidade: gestores/administradores veem tudo; organizações veem os validados e os próprios;
 * moradores veem apenas os validados. Pendentes nunca aparecem como confirmados.
 */
@Service
public class RecursoService {

    /** Filtros da consulta. Latitude, longitude e raio (km) devem vir juntos. */
    public record Filtro(CategoriaRecurso categoria, StatusRecurso status, String busca,
            Double latitude, Double longitude, Double raioKm) { }

    private static final double RAIO_MAXIMO_KM = 200;

    private final RecursoRepository recursos;
    private final OrganizacaoRepository organizacoes;
    private final UsuarioRepository usuarios;
    private final RegistroDisponibilidadeRepository disponibilidades;
    private final RecursoHistoricoRepository historicos;
    private final AuditoriaService auditoria;

    public RecursoService(RecursoRepository recursos, OrganizacaoRepository organizacoes, UsuarioRepository usuarios,
            RegistroDisponibilidadeRepository disponibilidades, RecursoHistoricoRepository historicos,
            AuditoriaService auditoria) {
        this.recursos = recursos;
        this.organizacoes = organizacoes;
        this.usuarios = usuarios;
        this.disponibilidades = disponibilidades;
        this.historicos = historicos;
        this.auditoria = auditoria;
    }

    @Transactional
    public RecursoResponse criar(RecursoRequest req, UsuarioPrincipal ator) {
        Organizacao org = resolverOrganizacao(req, ator);
        if (!org.estaAprovada()) {
            throw new BusinessException("A organização ainda não foi aprovada para cadastrar recursos.");
        }
        RecursoComunitario r = instanciar(req.categoria(), org);
        aplicarDados(r, req);
        r.atualizarDisponibilidade(req.capacidadeTotal(), req.capacidadeDisponivel());
        r = recursos.save(r);

        Usuario autor = usuarios.getReferenceById(ator.getId());
        disponibilidades.save(new RegistroDisponibilidade(r, req.capacidadeTotal(), req.capacidadeDisponivel(),
                "Cadastro inicial", autor));
        historicos.save(new RecursoHistorico(r, "CRIADO", "Recurso cadastrado e pendente de validação.", autor));
        return mapear(r, ator);
    }

    @Transactional
    public RecursoResponse atualizar(Long id, RecursoRequest req, UsuarioPrincipal ator) {
        RecursoComunitario r = carregar(id);
        exigirEscrita(r, ator);
        exigirAtivo(r);
        if (req.categoria() != r.getCategoria()) {
            throw new BusinessException("A categoria de um recurso não pode ser alterada.");
        }
        aplicarDados(r, req);
        if (!ator.isStaff()) {
            r.exigirNovaValidacao();
        }
        historicos.save(new RecursoHistorico(r, "ATUALIZADO",
                ator.isStaff() ? "Dados atualizados." : "Dados atualizados; recurso aguarda nova validação.",
                usuarios.getReferenceById(ator.getId())));
        return mapear(r, ator);
    }

    @Transactional
    public RecursoResponse atualizarDisponibilidade(Long id, DisponibilidadeRequest req, UsuarioPrincipal ator) {
        RecursoComunitario r = carregar(id);
        exigirEscrita(r, ator);
        exigirAtivo(r);
        BigDecimal total = req.capacidadeTotal() != null ? req.capacidadeTotal() : r.getCapacidadeTotal();
        r.atualizarDisponibilidade(total, req.capacidadeDisponivel());
        Usuario autor = usuarios.getReferenceById(ator.getId());
        disponibilidades.save(new RegistroDisponibilidade(r, total, req.capacidadeDisponivel(), req.observacao(), autor));
        historicos.save(new RecursoHistorico(r, "DISPONIBILIDADE_ATUALIZADA",
                "Disponível: " + req.capacidadeDisponivel().stripTrailingZeros().toPlainString() + " de "
                        + total.stripTrailingZeros().toPlainString() + " " + r.getUnidadeMedida(), autor));
        return mapear(r, ator);
    }

    @Transactional
    public RecursoResponse validar(Long id, UsuarioPrincipal ator) {
        RecursoComunitario r = carregar(id);
        if (!r.getOrganizacao().estaAprovada()) {
            throw new BusinessException("Aprove a organização responsável antes de validar o recurso.");
        }
        Usuario autor = usuarios.getReferenceById(ator.getId());
        r.validar(autor);
        historicos.save(new RecursoHistorico(r, "VALIDADO", "Recurso validado.", autor));
        auditoria.registrar(ator.getId(), "RECURSO_VALIDADO", "RecursoComunitario", id, r.getNome());
        return mapear(r, ator);
    }

    @Transactional
    public RecursoResponse rejeitar(Long id, String motivo, UsuarioPrincipal ator) {
        RecursoComunitario r = carregar(id);
        Usuario autor = usuarios.getReferenceById(ator.getId());
        r.rejeitar(autor);
        historicos.save(new RecursoHistorico(r, "REJEITADO", motivo != null ? motivo : "Recurso rejeitado.", autor));
        auditoria.registrar(ator.getId(), "RECURSO_REJEITADO", "RecursoComunitario", id, motivo);
        return mapear(r, ator);
    }

    /** "Excluir" é desativar: preserva histórico e registros de disponibilidade. */
    @Transactional
    public RecursoResponse desativar(Long id, UsuarioPrincipal ator) {
        RecursoComunitario r = carregar(id);
        exigirEscrita(r, ator);
        r.desativar();
        historicos.save(new RecursoHistorico(r, "DESATIVADO", "Recurso desativado.",
                usuarios.getReferenceById(ator.getId())));
        auditoria.registrar(ator.getId(), "RECURSO_DESATIVADO", "RecursoComunitario", id, r.getNome());
        return mapear(r, ator);
    }

    @Transactional(readOnly = true)
    public PageResponse<RecursoResponse> buscar(Filtro f, int pagina, int tamanho, UsuarioPrincipal ator) {
        Specification<RecursoComunitario> spec = escopo(ator);
        if (f.categoria() != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("categoria"), f.categoria()));
        }
        if (f.status() != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), f.status()));
        }
        if (f.busca() != null && !f.busca().isBlank()) {
            String padrao = "%" + f.busca().trim().toLowerCase().replace("\\", "\\\\").replace("%", "\\%")
                    .replace("_", "\\_") + "%";
            spec = spec.and((root, q, cb) -> cb.like(cb.lower(root.get("nome")), padrao, '\\'));
        }
        if (f.latitude() != null || f.longitude() != null || f.raioKm() != null) {
            if (f.latitude() == null || f.longitude() == null || f.raioKm() == null) {
                throw new BusinessException("Para filtrar por localização, informe latitude, longitude e raio (km).");
            }
            if (f.latitude() < -90 || f.latitude() > 90 || f.longitude() < -180 || f.longitude() > 180
                    || f.raioKm() <= 0 || f.raioKm() > RAIO_MAXIMO_KM) {
                throw new BusinessException("Coordenadas inválidas ou raio fora do intervalo (0 a " + (int) RAIO_MAXIMO_KM + " km).");
            }
            List<Long> ids = recursos.idsNoRaio(f.latitude(), f.longitude(), f.raioKm() * 1000);
            if (ids.isEmpty()) {
                return PageResponse.vazia(pagina, tamanho);
            }
            spec = spec.and((root, q, cb) -> root.get("id").in(ids));
        }
        var pageable = PageResponse.pageable(pagina, tamanho, Sort.by(Sort.Direction.DESC, "atualizadoEm"));
        return PageResponse.de(recursos.findAll(spec, pageable).map(r -> mapear(r, ator)));
    }

    @Transactional(readOnly = true)
    public RecursoResponse obter(Long id, UsuarioPrincipal ator) {
        RecursoComunitario r = carregar(id);
        boolean visivel = ator.isStaff() || ehDona(r, ator) || r.getStatus() == StatusRecurso.VALIDADO;
        if (!visivel) {
            throw new ResourceNotFoundException("Recurso não encontrado.");
        }
        return mapear(r, ator);
    }

    @Transactional(readOnly = true)
    public List<HistoricoRecursoResponse> historico(Long id, UsuarioPrincipal ator) {
        RecursoComunitario r = carregar(id);
        exigirEscrita(r, ator);
        return historicos.findByRecursoIdOrderByCriadoEmDesc(id).stream().map(RecursoMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<DisponibilidadeResponse> disponibilidades(Long id, int pagina, int tamanho,
            UsuarioPrincipal ator) {
        RecursoComunitario r = carregar(id);
        exigirEscrita(r, ator);
        var pageable = PageResponse.pageable(pagina, tamanho, Sort.unsorted());
        return PageResponse.de(disponibilidades.findByRecursoIdOrderByRegistradoEmDesc(id, pageable)
                .map(RecursoMapper::toResponse));
    }

    // ---- regras internas ----

    private Specification<RecursoComunitario> escopo(UsuarioPrincipal ator) {
        if (ator.isStaff()) {
            return (root, q, cb) -> cb.conjunction();
        }
        Specification<RecursoComunitario> validados =
                (root, q, cb) -> cb.equal(root.get("status"), StatusRecurso.VALIDADO);
        if (ator.getPerfil() == Perfil.ORGANIZACAO && ator.getOrganizacaoId() != null) {
            Long orgId = ator.getOrganizacaoId();
            return validados.or((root, q, cb) -> cb.equal(root.get("organizacao").get("id"), orgId));
        }
        return validados;
    }

    private Organizacao resolverOrganizacao(RecursoRequest req, UsuarioPrincipal ator) {
        Long orgId;
        if (ator.getPerfil() == Perfil.ORGANIZACAO) {
            // A organização é sempre a do usuário autenticado; o valor enviado no corpo é ignorado.
            orgId = ator.getOrganizacaoId();
        } else {
            orgId = req.organizacaoId();
        }
        if (orgId == null) {
            throw new BusinessException("Informe a organização responsável pelo recurso.");
        }
        return organizacoes.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organização não encontrada."));
    }

    private RecursoComunitario instanciar(CategoriaRecurso categoria, Organizacao org) {
        return switch (categoria) {
            case AGUA -> new PontoAgua(org);
            case ABRIGO -> new Abrigo(org);
            case ENERGIA -> new PontoEnergia(org);
            case ALIMENTACAO -> new PontoAlimentos(org);
            case COMUNICACAO, SAUDE -> new RecursoComunitario(categoria, org);
        };
    }

    private void aplicarDados(RecursoComunitario r, RecursoRequest req) {
        r.atualizarDados(req.nome().trim(), req.descricao(), req.endereco().trim(), req.latitude(), req.longitude(),
                req.unidadeMedida().trim(), req.horarioFuncionamento(), req.acessivel(), req.acessibilidadeDescricao());
        r.aplicarDetalhes(req.detalhes());
    }

    private boolean ehDona(RecursoComunitario r, UsuarioPrincipal ator) {
        return ator.getPerfil() == Perfil.ORGANIZACAO && ator.getOrganizacaoId() != null
                && ator.getOrganizacaoId().equals(r.getOrganizacao().getId());
    }

    /** Escrita: gestor/administrador, ou a organização dona do recurso. */
    void exigirEscrita(RecursoComunitario r, UsuarioPrincipal ator) {
        if (!ator.isStaff() && !ehDona(r, ator)) {
            throw new AccessDeniedException("Sem permissão sobre este recurso.");
        }
    }

    private void exigirAtivo(RecursoComunitario r) {
        if (r.getStatus() == StatusRecurso.INATIVO) {
            throw new BusinessException("Recurso inativo não pode ser alterado.");
        }
    }

    private RecursoResponse mapear(RecursoComunitario r, UsuarioPrincipal ator) {
        return RecursoMapper.toResponse(r, ator.isStaff() || ehDona(r, ator));
    }

    private RecursoComunitario carregar(Long id) {
        return recursos.findById(id).orElseThrow(() -> new ResourceNotFoundException("Recurso não encontrado."));
    }
}
