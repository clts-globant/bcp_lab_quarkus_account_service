package com.bank.account.dto;

import com.bank.account.entity.enums.AccountType;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class CreateAccountRequest {

  @NotNull
  public Long customerId;

  @NotNull
  public AccountType accountType;

  public BigDecimal initialBalance;

}
