package br.com.blockready.model.entity;

import java.util.Map;

import br.com.blockready.model.DetalhesRecurso;
import br.com.blockready.model.enums.CategoriaRecurso;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Abrigo: a capacidade é medida em vagas. */
@Entity
@Table(name = "abrigo")
public class Abrigo extends RecursoComunitario {

    @Column(name = "aceita_animais", nullable = false)
    private boolean aceitaAnimais;

    @Column(name = "possui_refeitorio", nullable = false)
    private boolean possuiRefeitorio;

    protected Abrigo() { }

    public Abrigo(Organizacao organizacao) { super(CategoriaRecurso.ABRIGO, organizacao); }

    @Override
    public void aplicarDetalhes(DetalhesRecurso d) {
        if (d == null) { return; }
        this.aceitaAnimais = Boolean.TRUE.equals(d.aceitaAnimais());
        this.possuiRefeitorio = Boolean.TRUE.equals(d.possuiRefeitorio());
    }

    @Override
    public Map<String, Object> detalhesEspecificos() {
        return mapa("aceitaAnimais", aceitaAnimais, "possuiRefeitorio", possuiRefeitorio);
    }

    @Override
    public String descreverCapacidade() {
        return getCapacidadeDisponivel().stripTrailingZeros().toPlainString() + " de "
                + getCapacidadeTotal().stripTrailingZeros().toPlainString() + " vagas livres";
    }

    public boolean isAceitaAnimais() { return aceitaAnimais; }
    public boolean isPossuiRefeitorio() { return possuiRefeitorio; }
}
