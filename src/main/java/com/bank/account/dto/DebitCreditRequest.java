package com.bank.account.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class DebitCreditRequest {

  @NotNull
  public BigDecimal amount;

  @NotNull
  public String transactionId;

  public String description;

}
