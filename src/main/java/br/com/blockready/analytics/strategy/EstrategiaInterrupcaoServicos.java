package br.com.blockready.analytics.strategy;

import java.util.Map;

import org.springframework.stereotype.Component;

import br.com.blockready.model.enums.CategoriaRecurso;

/** Interrupção de serviços essenciais: água e alimentação lideram, com saúde em seguida. */
@Component
public class EstrategiaInterrupcaoServicos extends PriorizacaoBase {

    @Override
    public String key() { return "priorizacao-interrupcao-servicos"; }

    @Override
    public String titulo() { return "Priorização para interrupção de serviços essenciais"; }

    @Override
    protected Map<CategoriaRecurso, Double> pesos() {
        return Map.of(
                CategoriaRecurso.AGUA, 1.0,
                CategoriaRecurso.ALIMENTACAO, 0.9,
                CategoriaRecurso.SAUDE, 0.8,
                CategoriaRecurso.COMUNICACAO, 0.6,
                CategoriaRecurso.ENERGIA, 0.5,
                CategoriaRecurso.ABRIGO, 0.3);
    }

    @Override
    protected String acaoPara(CategoriaRecurso categoria) {
        return switch (categoria) {
            case AGUA -> "Ativar pontos de distribuição de água enquanto o abastecimento estiver suspenso.";
            case ALIMENTACAO -> "Organizar distribuição de refeições e cestas nos pontos de apoio.";
            case SAUDE -> "Manter atendimento básico de saúde acessível à população.";
            case COMUNICACAO -> "Informar a população sobre a situação dos serviços e os pontos de apoio.";
            case ENERGIA -> "Verificar a disponibilidade de energia nos pontos de apoio.";
            case ABRIGO -> "Acolher pessoas em situação de vulnerabilidade agravada pela interrupção.";
        };
    }
}
