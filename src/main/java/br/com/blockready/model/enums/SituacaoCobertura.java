package br.com.blockready.model.enums;

import java.math.BigDecimal;

/**
 * Faixas visuais do Community Resilience Index. Os limiares são configuráveis por parâmetro;
 * cobertura de 100% indica capacidade compatível com a demanda estimada, nunca garantia de segurança.
 */
public enum SituacaoCobertura {
    SUFICIENTE,
    PARCIAL,
    INSUFICIENTE,
    SEM_DADOS;

    /** Classifica um percentual de cobertura pelos limiares informados (ex.: 90 e 50). */
    public static SituacaoCobertura classificar(BigDecimal percentual, BigDecimal limiarSuficiente,
            BigDecimal limiarParcial) {
        if (percentual == null) {
            return SEM_DADOS;
        }
        if (percentual.compareTo(limiarSuficiente) >= 0) {
            return SUFICIENTE;
        }
        if (percentual.compareTo(limiarParcial) >= 0) {
            return PARCIAL;
        }
        return INSUFICIENTE;
    }
}
