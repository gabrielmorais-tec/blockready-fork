package br.com.blockready.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.blockready.dto.Requests.RegistroRequest;
import br.com.blockready.dto.Responses.UsuarioResponse;
import br.com.blockready.exception.BusinessException;
import br.com.blockready.exception.ConflictException;
import br.com.blockready.mapper.OrganizacaoMapper;
import br.com.blockready.mapper.UsuarioMapper;
import br.com.blockready.model.entity.Organizacao;
import br.com.blockready.model.entity.Usuario;
import br.com.blockready.model.enums.Perfil;
import br.com.blockready.repository.OrganizacaoRepository;
import br.com.blockready.repository.UsuarioRepository;

@Service
public class AuthService {

    private final UsuarioRepository usuarios;
    private final OrganizacaoRepository organizacoes;
    private final PasswordEncoder encoder;
    private final AuditoriaService auditoria;

    public AuthService(UsuarioRepository usuarios, OrganizacaoRepository organizacoes, PasswordEncoder encoder,
            AuditoriaService auditoria) {
        this.usuarios = usuarios;
        this.organizacoes = organizacoes;
        this.encoder = encoder;
        this.auditoria = auditoria;
    }

    /** Cadastro público. Gestores e administradores só são criados por um administrador. */
    @Transactional
    public UsuarioResponse registrar(RegistroRequest r) {
        if (r.perfil() != Perfil.MORADOR && r.perfil() != Perfil.ORGANIZACAO) {
            throw new BusinessException("O cadastro público permite apenas os perfis MORADOR e ORGANIZACAO.");
        }
        String email = r.email().trim().toLowerCase();
        if (usuarios.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("E-mail já cadastrado.");
        }
        Organizacao org = null;
        if (r.perfil() == Perfil.ORGANIZACAO) {
            if (r.organizacao() == null) {
                throw new BusinessException("Informe os dados da organização.");
            }
            org = new Organizacao(r.organizacao().nome().trim(), r.organizacao().tipo(),
                    r.organizacao().responsavelNome().trim());
            OrganizacaoMapper.aplicar(org, r.organizacao());
            org = organizacoes.save(org);
        }
        Usuario u = usuarios.save(new Usuario(r.nome().trim(), email, encoder.encode(r.senha()), r.telefone(),
                r.perfil(), org));
        auditoria.registrar(u.getId(), "USUARIO_CADASTRADO", "Usuario", u.getId(), "perfil=" + r.perfil());
        return UsuarioMapper.toResponse(u);
    }
}
