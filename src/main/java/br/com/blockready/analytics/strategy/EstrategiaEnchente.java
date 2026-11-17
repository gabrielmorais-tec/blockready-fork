package br.com.blockready.analytics.strategy;

import java.util.Map;

import org.springframework.stereotype.Component;

import br.com.blockready.model.enums.CategoriaRecurso;

/** Enchente: prioriza abrigo e água (contaminação), com saúde e alimentação em seguida. */
@Component
public class EstrategiaEnchente extends PriorizacaoBase {

    @Override
    public String key() { return "priorizacao-enchente"; }

    @Override
    public String titulo() { return "Priorização para enchente"; }

    @Override
    protected Map<CategoriaRecurso, Double> pesos() {
        return Map.of(
                CategoriaRecurso.ABRIGO, 1.0,
                CategoriaRecurso.AGUA, 0.9,
                CategoriaRecurso.SAUDE, 0.8,
                CategoriaRecurso.ALIMENTACAO, 0.7,
                CategoriaRecurso.COMUNICACAO, 0.5,
                CategoriaRecurso.ENERGIA, 0.4);
    }

    @Override
    protected String acaoPara(CategoriaRecurso categoria) {
        return switch (categoria) {
            case ABRIGO -> "Abrir e ampliar vagas de abrigo fora das áreas alagáveis.";
            case AGUA -> "Mobilizar pontos de água potável: a rede pode estar contaminada pela cheia.";
            case SAUDE -> "Reforçar atendimento de saúde para feridos e doenças de veiculação hídrica.";
            case ALIMENTACAO -> "Organizar distribuição de refeições junto aos abrigos ativos.";
            case COMUNICACAO -> "Divulgar rotas seguras e pontos de apoio pelos canais comunitários.";
            case ENERGIA -> "Disponibilizar pontos de carregamento nos abrigos e pontos de apoio.";
        };
    }
}
