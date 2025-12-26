package com.bank.account.entity.enums;

public enum AccountType {
    CHECKING("Cuenta Corriente"),
    SAVINGS("Cuenta de Ahorros");

    private final String description;

    AccountType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
