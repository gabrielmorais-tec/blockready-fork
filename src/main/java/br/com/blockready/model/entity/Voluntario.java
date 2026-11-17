package br.com.blockready.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/** Disponibilidade de uma pessoa para ajudar. Associada ao usuário (composição), sem herança. */
@Entity
@Table(name = "voluntario")
public class Voluntario extends EntidadeBase {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    @Column(nullable = false, length = 1000)
    private String habilidades;

    @Column(nullable = false, length = 500)
    private String disponibilidade;

    @Column(nullable = false)
    private boolean ativo = true;

    protected Voluntario() { }

    public Voluntario(Usuario usuario) { this.usuario = usuario; }

    public void atualizar(String habilidades, String disponibilidade, boolean ativo) {
        this.habilidades = habilidades;
        this.disponibilidade = disponibilidade;
        this.ativo = ativo;
    }

    public Usuario getUsuario() { return usuario; }
    public String getHabilidades() { return habilidades; }
    public String getDisponibilidade() { return disponibilidade; }
    public boolean isAtivo() { return ativo; }
}
