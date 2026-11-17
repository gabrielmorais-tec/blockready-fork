package br.com.blockready.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import br.com.blockready.model.entity.Usuario;
import br.com.blockready.model.enums.Perfil;

/** Identidade autenticada guardada na sessão. O hash da senha é apagado após a autenticação. */
public class UsuarioPrincipal implements UserDetails, CredentialsContainer {

    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String nome;
    private final String email;
    private String senhaHash;
    private final Perfil perfil;
    private final Long organizacaoId;
    private final boolean ativo;

    public UsuarioPrincipal(Long id, String nome, String email, String senhaHash, Perfil perfil,
            Long organizacaoId, boolean ativo) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.senhaHash = senhaHash;
        this.perfil = perfil;
        this.organizacaoId = organizacaoId;
        this.ativo = ativo;
    }

    public static UsuarioPrincipal de(Usuario u) {
        Long orgId = u.getOrganizacao() != null ? u.getOrganizacao().getId() : null;
        return new UsuarioPrincipal(u.getId(), u.getNome(), u.getEmail(), u.getSenhaHash(), u.getPerfil(), orgId,
                u.isAtivo());
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public Perfil getPerfil() { return perfil; }
    public Long getOrganizacaoId() { return organizacaoId; }
    public boolean isStaff() { return perfil.isStaff(); }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(perfil.authority()));
    }
    @Override public String getPassword() { return senhaHash; }
    @Override public String getUsername() { return email; }
    @Override public boolean isEnabled() { return ativo; }
    @Override public void eraseCredentials() { this.senhaHash = null; }
}
