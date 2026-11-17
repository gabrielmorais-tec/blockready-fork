package br.com.blockready.model.enums;

public enum Perfil {
    MORADOR, ORGANIZACAO, GESTOR, ADMIN;

    public String authority() { return "ROLE_" + name(); }

    /** Perfis com funções administrativas da comunidade. */
    public boolean isStaff() { return this == GESTOR || this == ADMIN; }
}
