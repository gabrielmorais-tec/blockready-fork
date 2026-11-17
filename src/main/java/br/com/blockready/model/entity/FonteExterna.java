package br.com.blockready.model.entity;

import java.time.Instant;

import br.com.blockready.model.enums.AbrangenciaFonte;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/** Fonte oficial de dados importados (IBGE, CEMADEN, Atlas Digital de Desastres). */
@Entity
@Table(name = "fonte_externa")
public class FonteExterna extends EntidadeBase {

    @Column(nullable = false, length = 40, unique = true)
    private String codigo;

    @Column(nullable = false, length = 160)
    private String nome;

    @Column(columnDefinition = "text")
    private String descricao;

    @Column(name = "url_base", length = 300)
    private String urlBase;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AbrangenciaFonte abrangencia;

    @Column(name = "ultima_importacao")
    private Instant ultimaImportacao;

    protected FonteExterna() { }

    public void registrarImportacao() { this.ultimaImportacao = Instant.now(); }

    public String getCodigo() { return codigo; }
    public String getNome() { return nome; }
    public String getDescricao() { return descricao; }
    public String getUrlBase() { return urlBase; }
    public AbrangenciaFonte getAbrangencia() { return abrangencia; }
    public Instant getUltimaImportacao() { return ultimaImportacao; }
}
