package br.com.blockready.model.entity;

import java.util.Map;

import br.com.blockready.model.DetalhesRecurso;
import br.com.blockready.model.enums.CategoriaRecurso;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Ponto de energia: gerador, painel ou ponto de recarga. */
@Entity
@Table(name = "ponto_energia")
public class PontoEnergia extends RecursoComunitario {

    @Column(name = "tipo_fonte", length = 80)
    private String tipoFonte;

    @Column(name = "permite_carregar_dispositivos", nullable = false)
    private boolean permiteCarregarDispositivos;

    protected PontoEnergia() { }

    public PontoEnergia(Organizacao organizacao) { super(CategoriaRecurso.ENERGIA, organizacao); }

    @Override
    public void aplicarDetalhes(DetalhesRecurso d) {
        if (d == null) { return; }
        this.tipoFonte = d.tipoFonte();
        this.permiteCarregarDispositivos = Boolean.TRUE.equals(d.permiteCarregarDispositivos());
    }

    @Override
    public Map<String, Object> detalhesEspecificos() {
        return mapa("tipoFonte", tipoFonte, "permiteCarregarDispositivos", permiteCarregarDispositivos);
    }

    public String getTipoFonte() { return tipoFonte; }
    public boolean isPermiteCarregarDispositivos() { return permiteCarregarDispositivos; }
}
