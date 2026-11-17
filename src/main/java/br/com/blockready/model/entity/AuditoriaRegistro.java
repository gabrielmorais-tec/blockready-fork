package br.com.blockready.model.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Registro de operação sensível. Guarda o id do usuário (sem relacionamento carregado). */
@Entity
@Table(name = "auditoria_registro")
public class AuditoriaRegistro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(nullable = false, length = 60)
    private String acao;

    @Column(nullable = false, length = 60)
    private String entidade;

    @Column(name = "entidade_id")
    private Long entidadeId;

    @Column(length = 1000)
    private String detalhes;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm = Instant.now();

    protected AuditoriaRegistro() { }

    public AuditoriaRegistro(Long usuarioId, String acao, String entidade, Long entidadeId, String detalhes) {
        this.usuarioId = usuarioId;
        this.acao = acao;
        this.entidade = entidade;
        this.entidadeId = entidadeId;
        this.detalhes = detalhes;
    }

    public Long getId() { return id; }
    public Long getUsuarioId() { return usuarioId; }
    public String getAcao() { return acao; }
    public String getEntidade() { return entidade; }
    public Long getEntidadeId() { return entidadeId; }
    public String getDetalhes() { return detalhes; }
    public Instant getCriadoEm() { return criadoEm; }
}
