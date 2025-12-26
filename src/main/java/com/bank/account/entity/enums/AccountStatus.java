package com.bank.account.entity.enums;

public enum AccountStatus {
    ACTIVE("Active"),
    INACTIVE("Inactive");

    private final String description;

    AccountStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
