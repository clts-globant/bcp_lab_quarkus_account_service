package com.bank.account.dto;

import java.math.BigDecimal;

public class TransactionEvent {

  public String timestamp;
  public String sourceAccountId;
  public String targetAccountId;
  public BigDecimal amount;
  public String status;
  public String description;
  public String transactionId;


}
