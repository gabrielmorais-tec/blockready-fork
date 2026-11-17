package br.com.blockready.model.entity;

import br.com.blockready.model.enums.StatusAprovacao;
import br.com.blockready.model.enums.TipoOrganizacao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/** Organização parceira que cadastra e mantém recursos comunitários. */
@Entity
@Table(name = "organizacao")
public class Organizacao extends EntidadeBase {

    @Column(nullable = false, length = 160)
    private String nome;

    @Column(columnDefinition = "text")
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoOrganizacao tipo;

    @Column(name = "contato_email", length = 160)
    private String contatoEmail;

    @Column(name = "contato_telefone", length = 30)
    private String contatoTelefone;

    @Column(length = 300)
    private String endereco;

    private Double latitude;
    private Double longitude;

    @Column(name = "responsavel_nome", nullable = false, length = 120)
    private String responsavelNome;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_aprovacao", nullable = false, length = 20)
    private StatusAprovacao statusAprovacao = StatusAprovacao.PENDENTE;

    protected Organizacao() { }

    public Organizacao(String nome, TipoOrganizacao tipo, String responsavelNome) {
        this.nome = nome;
        this.tipo = tipo;
        this.responsavelNome = responsavelNome;
    }

    public void atualizarDados(String nome, String descricao, TipoOrganizacao tipo, String contatoEmail,
            String contatoTelefone, String endereco, Double latitude, Double longitude, String responsavelNome) {
        this.nome = nome;
        this.descricao = descricao;
        this.tipo = tipo;
        this.contatoEmail = contatoEmail;
        this.contatoTelefone = contatoTelefone;
        this.endereco = endereco;
        this.latitude = latitude;
        this.longitude = longitude;
        this.responsavelNome = responsavelNome;
    }

    public void aprovar() { this.statusAprovacao = StatusAprovacao.APROVADA; }
    public void rejeitar() { this.statusAprovacao = StatusAprovacao.REJEITADA; }
    public void suspender() { this.statusAprovacao = StatusAprovacao.SUSPENSA; }
    public boolean estaAprovada() { return statusAprovacao == StatusAprovacao.APROVADA; }

    public String getNome() { return nome; }
    public String getDescricao() { return descricao; }
    public TipoOrganizacao getTipo() { return tipo; }
    public String getContatoEmail() { return contatoEmail; }
    public String getContatoTelefone() { return contatoTelefone; }
    public String getEndereco() { return endereco; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public String getResponsavelNome() { return responsavelNome; }
    public StatusAprovacao getStatusAprovacao() { return statusAprovacao; }
}
