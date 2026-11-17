package br.com.blockready.analytics.strategy;

import java.util.Map;

import org.springframework.stereotype.Component;

import br.com.blockready.model.enums.CategoriaRecurso;

/** Interrupção de energia: energia e comunicação lideram; saúde depende de equipamentos elétricos. */
@Component
public class EstrategiaInterrupcaoEnergia extends PriorizacaoBase {

    @Override
    public String key() { return "priorizacao-interrupcao-energia"; }

    @Override
    public String titulo() { return "Priorização para interrupção de energia"; }

    @Override
    protected Map<CategoriaRecurso, Double> pesos() {
        return Map.of(
                CategoriaRecurso.ENERGIA, 1.0,
                CategoriaRecurso.COMUNICACAO, 0.9,
                CategoriaRecurso.SAUDE, 0.8,
                CategoriaRecurso.AGUA, 0.6,
                CategoriaRecurso.ALIMENTACAO, 0.6,
                CategoriaRecurso.ABRIGO, 0.3);
    }

    @Override
    protected String acaoPara(CategoriaRecurso categoria) {
        return switch (categoria) {
            case ENERGIA -> "Mobilizar geradores e pontos de carregamento para a população.";
            case COMUNICACAO -> "Garantir comunicação independente da rede elétrica (rádio, pontos físicos).";
            case SAUDE -> "Priorizar pessoas que dependem de equipamentos elétricos de saúde.";
            case AGUA -> "Monitorar o abastecimento: bombas de água podem parar sem energia.";
            case ALIMENTACAO -> "Orientar sobre conservação de alimentos e organizar refeições coletivas.";
            case ABRIGO -> "Preparar abrigos com energia para quem precisar de suporte.";
        };
    }
}
