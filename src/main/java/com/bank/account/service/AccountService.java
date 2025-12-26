package com.bank.account.service;

import com.bank.account.entity.enums.AccountStatus;
import com.bank.account.entity.enums.AccountType;
import com.bank.account.exception.InactiveAccountException;
import com.bank.account.exception.InsufficientFundsException;
import com.bank.account.entity.Account;
import com.bank.account.exception.AccountNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class AccountService {

    @Transactional
    public Account createAccount(@NotNull Long customerId, @NotNull AccountType accountType, BigDecimal initialBalance) {
        Account account = new Account();
        account.accountNumber = generateAccountNumber();
        account.customerId = customerId;
        account.type = accountType;
        account.status = AccountStatus.ACTIVE;
        account.balance = initialBalance != null ? initialBalance : BigDecimal.ZERO;
        account.persist();
        return account;
    }

    @Transactional
    public Account createAccount(@NotNull Long customerId, @NotNull AccountType accountType) {
        return createAccount(customerId, accountType, null);
    }

    public Optional<Account> findByAccountNumber(@NotNull String accountNumber) {
        Account account = Account.findByAccountNumber(accountNumber);
        return Optional.ofNullable(account);
    }

    public List<Account> findByCustomerId(@NotNull Long customerId) {
        return Account.findByCustomerId(customerId);
    }

    @Transactional
    public void updateBalance(@NotNull String accountNumber, @NotNull BigDecimal delta) {
        Account account = Account.findByAccountNumber(accountNumber);
        if (account == null) {
            throw new AccountNotFoundException("Account not found: " + accountNumber);
        }

        if (account.status != AccountStatus.ACTIVE) {
            throw new InactiveAccountException("Account is not active: " + accountNumber);
        }

        BigDecimal newBalance = account.balance.add(delta);
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new InsufficientFundsException("Insufficient funds for account: " + accountNumber);
        }

        account.balance = newBalance;
        account.updatedAt = LocalDateTime.now();
        account.persist();
    }

    @Transactional
    public Account debitAccount(@NotNull String accountNumber, @NotNull BigDecimal amount, @NotNull String transactionId) {
        Account account = Account.findByAccountNumber(accountNumber);
        if (account == null) {
            throw new AccountNotFoundException("Account not found: " + accountNumber);
        }

        if (account.status != AccountStatus.ACTIVE) {
            throw new InactiveAccountException("Account is not active: " + accountNumber);
        }

        BigDecimal newBalance = account.balance.subtract(amount);
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new InsufficientFundsException("Insufficient funds for account: " + accountNumber);
        }

        account.balance = newBalance;
        account.updatedAt = LocalDateTime.now();
        account.persist();
        return account;
    }

    @Transactional
    public Account creditAccount(@NotNull String accountNumber, @NotNull BigDecimal amount, @NotNull String transactionId) {
        Account account = Account.findByAccountNumber(accountNumber);
        if (account == null) {
            throw new AccountNotFoundException("Account not found: " + accountNumber);
        }

        if (account.status != AccountStatus.ACTIVE) {
            throw new InactiveAccountException("Account is not active: " + accountNumber);
        }

        account.balance = account.balance.add(amount);
        account.updatedAt = LocalDateTime.now();
        account.persist();
        return account;
    }

    public boolean validateBalance(@NotNull String accountNumber, @Positive BigDecimal amount) {
        Account account = Account.findByAccountNumber(accountNumber);
        if (account == null) {
            return false;
        }

        return account.status == AccountStatus.ACTIVE && account.hasBalance(amount);
    }

    private String generateAccountNumber() {
        // Generate a unique 16-digit account number
        return String.format("%016d", Math.abs(UUID.randomUUID().getLeastSignificantBits() % 10000000000000000L));
    }
}
