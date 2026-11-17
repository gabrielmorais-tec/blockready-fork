package br.com.blockready.model.entity;

import java.util.Map;

import br.com.blockready.model.DetalhesRecurso;
import br.com.blockready.model.enums.CategoriaRecurso;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Ponto de água: informa se a água é potável e a origem da fonte. */
@Entity
@Table(name = "ponto_agua")
public class PontoAgua extends RecursoComunitario {

    @Column(nullable = false)
    private boolean potavel;

    @Column(name = "tipo_fonte", length = 80)
    private String tipoFonte;

    protected PontoAgua() { }

    public PontoAgua(Organizacao organizacao) { super(CategoriaRecurso.AGUA, organizacao); }

    @Override
    public void aplicarDetalhes(DetalhesRecurso d) {
        if (d == null) { return; }
        this.potavel = Boolean.TRUE.equals(d.potavel());
        this.tipoFonte = d.tipoFonte();
    }

    @Override
    public Map<String, Object> detalhesEspecificos() { return mapa("potavel", potavel, "tipoFonte", tipoFonte); }

    @Override
    public String descreverCapacidade() { return super.descreverCapacidade() + (potavel ? " (água potável)" : " (uso não potável)"); }

    public boolean isPotavel() { return potavel; }
    public String getTipoFonte() { return tipoFonte; }
}
