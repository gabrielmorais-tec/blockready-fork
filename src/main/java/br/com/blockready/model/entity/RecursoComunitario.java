package br.com.blockready.model.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import br.com.blockready.exception.BusinessException;
import br.com.blockready.model.DetalhesRecurso;
import br.com.blockready.model.enums.CategoriaRecurso;
import br.com.blockready.model.enums.StatusRecurso;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Recurso comunitário. Água, abrigo, energia e alimentação têm subtipos com atributos próprios;
 * comunicação e saúde usam esta classe diretamente (sem atributos específicos, logo sem herança forçada).
 *
 * <p>Estado e regras ficam encapsulados: as mudanças passam por métodos que validam as invariantes.</p>
 */
@Entity
@Table(name = "recurso_comunitario")
@Inheritance(strategy = InheritanceType.JOINED)
public class RecursoComunitario extends EntidadeBase {

    @Column(nullable = false, length = 160)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private CategoriaRecurso categoria;

    @Column(columnDefinition = "text")
    private String descricao;

    @Column(nullable = false, length = 300)
    private String endereco;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(name = "capacidade_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal capacidadeTotal = BigDecimal.ZERO;

    @Column(name = "capacidade_disponivel", nullable = false, precision = 12, scale = 2)
    private BigDecimal capacidadeDisponivel = BigDecimal.ZERO;

    @Column(name = "unidade_medida", nullable = false, length = 30)
    private String unidadeMedida;

    @Column(name = "horario_funcionamento", length = 200)
    private String horarioFuncionamento;

    @Column(nullable = false)
    private boolean acessivel;

    @Column(name = "acessibilidade_descricao", length = 300)
    private String acessibilidadeDescricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private StatusRecurso status = StatusRecurso.PENDENTE_VALIDACAO;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organizacao_id", nullable = false)
    private Organizacao organizacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validado_por_id")
    private Usuario validadoPor;

    @Column(name = "validado_em")
    private Instant validadoEm;

    protected RecursoComunitario() { }

    public RecursoComunitario(CategoriaRecurso categoria, Organizacao organizacao) {
        this.categoria = categoria;
        this.organizacao = organizacao;
    }

    public void atualizarDados(String nome, String descricao, String endereco, double latitude, double longitude,
            String unidadeMedida, String horarioFuncionamento, boolean acessivel, String acessibilidadeDescricao) {
        this.nome = nome;
        this.descricao = descricao;
        this.endereco = endereco;
        this.latitude = latitude;
        this.longitude = longitude;
        this.unidadeMedida = unidadeMedida;
        this.horarioFuncionamento = horarioFuncionamento;
        this.acessivel = acessivel;
        this.acessibilidadeDescricao = acessibilidadeDescricao;
    }

    /** Atualiza capacidade e disponibilidade garantindo 0 <= disponível <= total. */
    public void atualizarDisponibilidade(BigDecimal total, BigDecimal disponivel) {
        if (total == null || disponivel == null || total.signum() < 0 || disponivel.signum() < 0) {
            throw new BusinessException("Capacidades não podem ser negativas.");
        }
        if (disponivel.compareTo(total) > 0) {
            throw new BusinessException("A capacidade disponível não pode ser maior que a capacidade total.");
        }
        this.capacidadeTotal = total;
        this.capacidadeDisponivel = disponivel;
    }

    /** Polimorfismo: subtipos aplicam seus próprios atributos. */
    public void aplicarDetalhes(DetalhesRecurso detalhes) { }

    /** Polimorfismo: subtipos expõem seus atributos específicos. */
    public Map<String, Object> detalhesEspecificos() { return new LinkedHashMap<>(); }

    /** Polimorfismo: cada subtipo descreve sua capacidade com a linguagem do domínio. */
    public String descreverCapacidade() {
        return capacidadeDisponivel.stripTrailingZeros().toPlainString() + " de "
                + capacidadeTotal.stripTrailingZeros().toPlainString() + " " + unidadeMedida + " disponíveis";
    }

    public void validar(Usuario por) {
        if (status != StatusRecurso.PENDENTE_VALIDACAO) {
            throw new BusinessException("Apenas recursos pendentes de validação podem ser validados.");
        }
        this.status = StatusRecurso.VALIDADO;
        this.validadoPor = por;
        this.validadoEm = Instant.now();
    }

    public void rejeitar(Usuario por) {
        if (status != StatusRecurso.PENDENTE_VALIDACAO) {
            throw new BusinessException("Apenas recursos pendentes de validação podem ser rejeitados.");
        }
        this.status = StatusRecurso.REJEITADO;
        this.validadoPor = por;
        this.validadoEm = Instant.now();
    }

    /** Edição de dados cadastrais por quem não valida: o recurso volta para validação. */
    public void exigirNovaValidacao() {
        if (status == StatusRecurso.VALIDADO || status == StatusRecurso.REJEITADO) {
            this.status = StatusRecurso.PENDENTE_VALIDACAO;
            this.validadoPor = null;
            this.validadoEm = null;
        }
    }

    public void desativar() {
        if (status == StatusRecurso.INATIVO) {
            throw new BusinessException("O recurso já está inativo.");
        }
        this.status = StatusRecurso.INATIVO;
    }

    public boolean estaConfirmado() { return status == StatusRecurso.VALIDADO; }

    protected static Map<String, Object> mapa(Object... paresChaveValor) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < paresChaveValor.length; i += 2) {
            m.put((String) paresChaveValor[i], paresChaveValor[i + 1]);
        }
        return m;
    }

    public String getNome() { return nome; }
    public CategoriaRecurso getCategoria() { return categoria; }
    public String getDescricao() { return descricao; }
    public String getEndereco() { return endereco; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public BigDecimal getCapacidadeTotal() { return capacidadeTotal; }
    public BigDecimal getCapacidadeDisponivel() { return capacidadeDisponivel; }
    public String getUnidadeMedida() { return unidadeMedida; }
    public String getHorarioFuncionamento() { return horarioFuncionamento; }
    public boolean isAcessivel() { return acessivel; }
    public String getAcessibilidadeDescricao() { return acessibilidadeDescricao; }
    public StatusRecurso getStatus() { return status; }
    public Organizacao getOrganizacao() { return organizacao; }
    public Usuario getValidadoPor() { return validadoPor; }
    public Instant getValidadoEm() { return validadoEm; }
}
