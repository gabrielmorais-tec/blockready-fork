package br.com.blockready.model.entity;

import java.util.Map;

import br.com.blockready.model.DetalhesRecurso;
import br.com.blockready.model.enums.CategoriaRecurso;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Ponto de alimentos: informa se há refrigeração e o tipo de alimento. */
@Entity
@Table(name = "ponto_alimentos")
public class PontoAlimentos extends RecursoComunitario {

    @Column(nullable = false)
    private boolean refrigeracao;

    @Column(name = "tipo_alimento", length = 80)
    private String tipoAlimento;

    protected PontoAlimentos() { }

    public PontoAlimentos(Organizacao organizacao) { super(CategoriaRecurso.ALIMENTACAO, organizacao); }

    @Override
    public void aplicarDetalhes(DetalhesRecurso d) {
        if (d == null) { return; }
        this.refrigeracao = Boolean.TRUE.equals(d.refrigeracao());
        this.tipoAlimento = d.tipoAlimento();
    }

    @Override
    public Map<String, Object> detalhesEspecificos() { return mapa("refrigeracao", refrigeracao, "tipoAlimento", tipoAlimento); }

    public boolean isRefrigeracao() { return refrigeracao; }
    public String getTipoAlimento() { return tipoAlimento; }
}
