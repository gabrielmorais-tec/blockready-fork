package br.com.blockready.model.entity;

import br.com.blockready.model.enums.Perfil;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Usuário do sistema. Morador, Organização, Gestor e Administrador são {@link Perfil perfis} (composição),
 * não subclasses: não possuem atributos próprios que justifiquem herança.
 */
@Entity
@Table(name = "usuario")
public class Usuario extends EntidadeBase {

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, length = 160)
    private String email;

    @Column(name = "senha_hash", nullable = false, length = 100)
    private String senhaHash;

    @Column(length = 30)
    private String telefone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Perfil perfil;

    @Column(nullable = false)
    private boolean ativo = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizacao_id")
    private Organizacao organizacao;

    protected Usuario() { }

    public Usuario(String nome, String email, String senhaHash, String telefone, Perfil perfil, Organizacao organizacao) {
        this.nome = nome;
        this.email = email;
        this.senhaHash = senhaHash;
        this.telefone = telefone;
        this.perfil = perfil;
        this.organizacao = organizacao;
    }

    public void alterarPerfil(Perfil novo) { this.perfil = novo; }
    public void definirAtivo(boolean ativo) { this.ativo = ativo; }

    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public String getSenhaHash() { return senhaHash; }
    public String getTelefone() { return telefone; }
    public Perfil getPerfil() { return perfil; }
    public boolean isAtivo() { return ativo; }
    public Organizacao getOrganizacao() { return organizacao; }
}
