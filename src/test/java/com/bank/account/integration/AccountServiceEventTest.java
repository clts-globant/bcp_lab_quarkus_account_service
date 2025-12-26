package com.bank.account.integration;

import com.bank.account.service.AccountService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import io.smallrye.reactive.messaging.memory.InMemorySource;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.times;

/**
 * Account microservices tests focused on transaction events consumption
 */
@QuarkusTest
public class AccountServiceEventTest {

  @Inject
  @Any
  InMemoryConnector connector;

  @InjectMock
  AccountService accountServiceMock;

  @BeforeAll
  public static void switchMyChannels() {
    InMemoryConnector.switchIncomingChannelsToInMemory("transactionss");
    InMemoryConnector.switchOutgoingChannelsToInMemory("transactionss");
  }

  @Test
  void testProcessTransactionEvent_whenTransactionCompleted() {
    InMemorySource<String> input = connector.source("transactionss");
    String originAccount = "ACC-001";
    String destinationAccount = "ACC-002";
    BigDecimal amount = new BigDecimal("250.75");
    String eventJson = String.format(
            "{\"eventType\":\"TRANSACTION_COMPLETED\", \"originAccount\":\"%s\", \"destinationAccount\":\"%s\", \"amount\":\"%s\", \"transactionId\":\"tx-123\"}",
            originAccount, destinationAccount, amount
                                    );
    input.send(eventJson);

    await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
      verify(accountServiceMock, times(1)).updateBalance(originAccount, amount.negate());
      verify(accountServiceMock, times(1)).updateBalance(destinationAccount, amount);
    });
  }

  @Test
  void testProcessTransactionEvent_whenTransactionFailed() {
    String eventJson = "{\"eventType\":\"TRANSACTION_FAILED\", \"transactionId\":\"tx-456\", \"reason\":\"Insufficient funds\"}";
    InMemorySource<String> input = connector.source("transactionss");
    input.send(eventJson);

    await().pollDelay(1, TimeUnit.SECONDS).until(() -> true);
    verifyNoInteractions(accountServiceMock);
  }

  @Test
  void testProcessTransactionEvent_whenEventTypeIsUnknown() {
    InMemorySource<String> input = connector.source("transactionss");
    String eventJson = "{\"eventType\":\"TRANSACTION_PENDING\", \"transactionId\":\"tx-789\"}";
    input.send(eventJson);
    await().pollDelay(1, TimeUnit.SECONDS).until(() -> true);
    verifyNoInteractions(accountServiceMock);
  }

  @Test
  void testProcessTransactionEvent_whenJsonIsMalformed() {
    InMemorySource<String> input = connector.source("transactionss");
    String malformedJson = "{\"eventType\":\"TRANSACTION_COMPLETED\", "; // Invalid JSON
    input.send(malformedJson);
    await().pollDelay(1, TimeUnit.SECONDS).until(() -> true);
    verifyNoInteractions(accountServiceMock);
  }
}
