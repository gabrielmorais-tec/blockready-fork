package br.com.blockready.service;

import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.blockready.dto.Requests.CriarUsuarioRequest;
import br.com.blockready.dto.Responses.MeResponse;
import br.com.blockready.dto.Responses.PageResponse;
import br.com.blockready.dto.Responses.UsuarioResponse;
import br.com.blockready.exception.BusinessException;
import br.com.blockready.exception.ConflictException;
import br.com.blockready.exception.ResourceNotFoundException;
import br.com.blockready.mapper.UsuarioMapper;
import br.com.blockready.model.entity.Usuario;
import br.com.blockready.model.enums.Perfil;
import br.com.blockready.repository.UsuarioRepository;
import br.com.blockready.security.UsuarioPrincipal;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final AuditoriaService auditoria;

    public UsuarioService(UsuarioRepository usuarios, PasswordEncoder encoder, AuditoriaService auditoria) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public MeResponse me(UsuarioPrincipal p) {
        Usuario u = carregar(p.getId());
        return new MeResponse(u.getId(), u.getNome(), u.getEmail(), u.getPerfil(),
                u.getOrganizacao() != null ? u.getOrganizacao().getId() : null,
                u.getOrganizacao() != null ? u.getOrganizacao().getStatusAprovacao() : null);
    }

    @Transactional(readOnly = true)
    public PageResponse<UsuarioResponse> listar(int pagina, int tamanho) {
        return PageResponse.de(usuarios
                .findAll(PageResponse.pageable(pagina, tamanho, Sort.by(Sort.Direction.DESC, "criadoEm")))
                .map(UsuarioMapper::toResponse));
    }

    @Transactional
    public UsuarioResponse criar(CriarUsuarioRequest r, UsuarioPrincipal ator) {
        if (r.perfil() == Perfil.ORGANIZACAO) {
            throw new BusinessException("Organizações se cadastram pelo formulário público, com seus dados institucionais.");
        }
        String email = r.email().trim().toLowerCase();
        if (usuarios.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("E-mail já cadastrado.");
        }
        Usuario u = usuarios.save(new Usuario(r.nome().trim(), email, encoder.encode(r.senha()), r.telefone(),
                r.perfil(), null));
        auditoria.registrar(ator.getId(), "USUARIO_CRIADO_POR_ADMIN", "Usuario", u.getId(), "perfil=" + r.perfil());
        return UsuarioMapper.toResponse(u);
    }

    @Transactional
    public UsuarioResponse alterarPerfil(Long id, Perfil novo, UsuarioPrincipal ator) {
        Usuario u = carregar(id);
        if (u.getId().equals(ator.getId())) {
            throw new BusinessException("Você não pode alterar o próprio perfil.");
        }
        if (novo == Perfil.ORGANIZACAO || u.getPerfil() == Perfil.ORGANIZACAO) {
            throw new BusinessException("O perfil ORGANIZACAO está vinculado a uma organização e não pode ser atribuído ou removido aqui.");
        }
        Perfil anterior = u.getPerfil();
        u.alterarPerfil(novo);
        auditoria.registrar(ator.getId(), "PERFIL_ALTERADO", "Usuario", id, anterior + " -> " + novo);
        return UsuarioMapper.toResponse(u);
    }

    @Transactional
    public UsuarioResponse alterarAtivo(Long id, boolean ativo, UsuarioPrincipal ator) {
        Usuario u = carregar(id);
        if (u.getId().equals(ator.getId())) {
            throw new BusinessException("Você não pode desativar a própria conta.");
        }
        u.definirAtivo(ativo);
        auditoria.registrar(ator.getId(), ativo ? "USUARIO_ATIVADO" : "USUARIO_DESATIVADO", "Usuario", id, null);
        return UsuarioMapper.toResponse(u);
    }

    private Usuario carregar(Long id) {
        return usuarios.findById(id).orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));
    }
}
