package com.bank.account.entity;

import com.bank.account.entity.enums.AccountStatus;
import com.bank.account.entity.enums.AccountType;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "accounts")
public class Account extends PanacheEntity {

    @Column(unique = true, nullable = false, length = 20)
    @NotNull
    public String accountNumber;

    @Column(nullable = false)
    @NotNull
    public Long customerId;

    @Column(nullable = false, precision = 15, scale = 2)
    @PositiveOrZero
    public BigDecimal balance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull
    public AccountType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull
    public AccountStatus status;

    @Column(nullable = false)
    public LocalDateTime createdAt;

    @Column(nullable = false)
    public LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public static Account findByAccountNumber(String accountNumber) {
        return find("accountNumber", accountNumber).firstResult();
    }

    public static List<Account> findByCustomerId(Long customerId) {
        return find("customerId", customerId).list();
    }

    public boolean hasBalance(BigDecimal amount) {
        return this.balance.compareTo(amount) >= 0;
    }


}
