package br.com.blockready.model.entity;

import java.time.Instant;

import br.com.blockready.model.enums.StatusImportacao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * Registro de uma importação de dataset externo. A lista dessas importações é o histórico
 * de importações do sistema (fonte, arquivo, período, totais, status e erros encontrados).
 */
@Entity
@Table(name = "importacao_dataset")
public class ImportacaoDataset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fonte_id", nullable = false)
    private FonteExterna fonte;

    @Column(name = "nome_arquivo", nullable = false, length = 260)
    private String nomeArquivo;

    @Column(name = "hash_arquivo", nullable = false, length = 64)
    private String hashArquivo;

    @Column(name = "periodo_referencia", length = 60)
    private String periodoReferencia;

    /** Dados demonstrativos recebem identificação explícita em toda a interface. */
    @Column(nullable = false)
    private boolean demonstrativo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusImportacao status;

    @Column(name = "total_processados", nullable = false)
    private int totalProcessados;

    @Column(name = "total_rejeitados", nullable = false)
    private int totalRejeitados;

    @Column(columnDefinition = "text")
    private String erros;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "importado_por_id", nullable = false)
    private Usuario importadoPor;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    protected ImportacaoDataset() { }

    public ImportacaoDataset(FonteExterna fonte, String nomeArquivo, String hashArquivo,
            String periodoReferencia, boolean demonstrativo, Usuario importadoPor) {
        this.fonte = fonte;
        this.nomeArquivo = nomeArquivo;
        this.hashArquivo = hashArquivo;
        this.periodoReferencia = periodoReferencia;
        this.demonstrativo = demonstrativo;
        this.importadoPor = importadoPor;
        this.status = StatusImportacao.PROCESSANDO;
    }

    @PrePersist
    void aoCriar() { this.criadoEm = Instant.now(); }

    public void concluir(int processados, int rejeitados, String erros) {
        this.totalProcessados = processados;
        this.totalRejeitados = rejeitados;
        this.erros = erros;
        this.status = rejeitados > 0 ? StatusImportacao.CONCLUIDA_COM_REJEICOES : StatusImportacao.CONCLUIDA;
    }

    public void falhar(String motivo) {
        this.status = StatusImportacao.FALHA;
        this.erros = motivo;
    }

    public Long getId() { return id; }
    public FonteExterna getFonte() { return fonte; }
    public String getNomeArquivo() { return nomeArquivo; }
    public String getHashArquivo() { return hashArquivo; }
    public String getPeriodoReferencia() { return periodoReferencia; }
    public boolean isDemonstrativo() { return demonstrativo; }
    public StatusImportacao getStatus() { return status; }
    public int getTotalProcessados() { return totalProcessados; }
    public int getTotalRejeitados() { return totalRejeitados; }
    public String getErros() { return erros; }
    public Usuario getImportadoPor() { return importadoPor; }
    public Instant getCriadoEm() { return criadoEm; }
}
