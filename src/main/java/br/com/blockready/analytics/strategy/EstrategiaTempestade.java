package br.com.blockready.analytics.strategy;

import java.util.Map;

import org.springframework.stereotype.Component;

import br.com.blockready.model.enums.CategoriaRecurso;

/** Tempestade intensa: abrigo e saúde primeiro (destelhamentos, feridos), energia em seguida. */
@Component
public class EstrategiaTempestade extends PriorizacaoBase {

    @Override
    public String key() { return "priorizacao-tempestade"; }

    @Override
    public String titulo() { return "Priorização para tempestade intensa"; }

    @Override
    protected Map<CategoriaRecurso, Double> pesos() {
        return Map.of(
                CategoriaRecurso.ABRIGO, 1.0,
                CategoriaRecurso.SAUDE, 0.9,
                CategoriaRecurso.ENERGIA, 0.8,
                CategoriaRecurso.COMUNICACAO, 0.7,
                CategoriaRecurso.AGUA, 0.5,
                CategoriaRecurso.ALIMENTACAO, 0.5);
    }

    @Override
    protected String acaoPara(CategoriaRecurso categoria) {
        return switch (categoria) {
            case ABRIGO -> "Acolher famílias com moradias danificadas pela tempestade.";
            case SAUDE -> "Preparar atendimento para feridos por queda de árvores e estruturas.";
            case ENERGIA -> "Ativar geradores e pontos de carregamento nas áreas sem energia.";
            case COMUNICACAO -> "Manter canais de alerta ativos enquanto durar a instabilidade.";
            case AGUA -> "Garantir reserva de água potável caso o abastecimento seja interrompido.";
            case ALIMENTACAO -> "Preparar refeições para famílias desalojadas.";
        };
    }
}
