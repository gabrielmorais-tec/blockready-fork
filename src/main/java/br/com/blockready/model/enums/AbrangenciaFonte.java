package br.com.blockready.model.enums;

/**
 * Abrangência geográfica dos registros de uma fonte externa.
 * Evita atribuir dados além da sua representatividade espacial: registros do Atlas valem para o
 * município inteiro; medições do CEMADEN valem para o ponto da estação; dados do IBGE, para o setor.
 */
public enum AbrangenciaFonte {
    SETOR_CENSITARIO,
    ESTACAO,
    MUNICIPIO
}
