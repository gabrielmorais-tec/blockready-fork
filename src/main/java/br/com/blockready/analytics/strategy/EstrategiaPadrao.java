package br.com.blockready.analytics.strategy;

import java.util.Map;

import org.springframework.stereotype.Component;

import br.com.blockready.model.enums.CategoriaRecurso;

/** Priorização padrão (sem cenário específico): pesos iguais, guiada apenas pela cobertura. */
@Component
public class EstrategiaPadrao extends PriorizacaoBase {

    public static final String KEY = "priorizacao-padrao";

    @Override
    public String key() { return KEY; }

    @Override
    public String titulo() { return "Priorização padrão (pesos iguais)"; }

    @Override
    protected Map<CategoriaRecurso, Double> pesos() {
        return Map.of(
                CategoriaRecurso.AGUA, 0.7,
                CategoriaRecurso.ABRIGO, 0.7,
                CategoriaRecurso.ENERGIA, 0.7,
                CategoriaRecurso.ALIMENTACAO, 0.7,
                CategoriaRecurso.COMUNICACAO, 0.7,
                CategoriaRecurso.SAUDE, 0.7);
    }

    @Override
    protected String acaoPara(CategoriaRecurso categoria) {
        return "Ampliar a capacidade de " + rotulo(categoria) + " ou cadastrar novos pontos de apoio.";
    }
}
