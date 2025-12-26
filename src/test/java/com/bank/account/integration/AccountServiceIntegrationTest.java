package com.bank.account.integration;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import io.smallrye.reactive.messaging.memory.InMemorySource;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.hasSize;

/**
 * Integration tests for Account Service endpoints.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AccountServiceIntegrationTest {

    @Inject
    @Any
    InMemoryConnector connectorIntegration;

    private static String createdAccountNumber;
    private static String secondAccountNumber;
    private static final Long TEST_CUSTOMER_ID = 1L;

    @Test
    @Order(1)
    @TestSecurity(user = "admin", roles = {"ROLE_ADMIN"})
    public void testCreateCheckingAccount() {
        String requestBody = """
            {
                "customerId": %d,
                "accountType": "CHECKING"
            }
            """.formatted(TEST_CUSTOMER_ID);

        createdAccountNumber = given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when().post("/api/accounts")
                .then()
                .statusCode(201)
                .body("accountNumber", notNullValue())
                .body("customerId", is(TEST_CUSTOMER_ID.intValue()))
                .body("type", is("CHECKING"))
                .body("status", is("ACTIVE"))
                .body("balance", is(0))
                .extract().path("accountNumber");
    }

    @Test
    @Order(2)
    @TestSecurity(user = "admin", roles = {"ROLE_ADMIN"})
    public void testCreateSavingsAccount() {
        String requestBody = """
            {
                "customerId": %d,
                "accountType": "SAVINGS",
                "initialBalance": 500.00
            }
            """.formatted(TEST_CUSTOMER_ID);

        secondAccountNumber = given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when().post("/api/accounts")
                .then()
                .statusCode(201)
                .body("accountNumber", notNullValue())
                .body("customerId", is(TEST_CUSTOMER_ID.intValue()))
                .body("type", is("SAVINGS"))
                .body("status", is("ACTIVE"))
                .body("balance", is(500.0f))
                .extract().path("accountNumber");
    }

    @Test
    @Order(3)
    @TestSecurity(user = "user", roles = {"ROLE_USER"})
    public void testGetAccount() {
        if (createdAccountNumber != null) {
            given()
                    .when().get("/api/accounts/{accountNumber}", createdAccountNumber)
                    .then()
                    .statusCode(200)
                    .body("accountNumber", is(createdAccountNumber))
                    .body("customerId", is(TEST_CUSTOMER_ID.intValue()))
                    .body("type", is("CHECKING"))
                    .body("status", is("ACTIVE"))
                    .body("balance", is(0.0f));
        }
    }

    @Test
    @Order(4)
    @TestSecurity(user = "user", roles = {"ROLE_USER"})
    public void testGetCustomerAccounts() {
        given()
                .when().get("/api/accounts/customer/{customerId}", TEST_CUSTOMER_ID)
                .then()
                .statusCode(200)
                .body("$", hasSize(4))
                .body("[0].customerId", is(TEST_CUSTOMER_ID.intValue()))
                .body("[1].customerId", is(TEST_CUSTOMER_ID.intValue()));
    }

    @Test
    @Order(5)
    @TestSecurity(user = "user", roles = {"ROLE_USER"})
    public void testCheckBalance() {
        if (createdAccountNumber != null) {
            given()
                    .when().get("/api/accounts/{accountNumber}/balance", createdAccountNumber)
                    .then()
                    .statusCode(200)
                    .body("accountNumber", is(createdAccountNumber))
                    .body("balance", is(0.0f))
                    .body("status", is("ACTIVE"));
        }
    }

    @Test
    @Order(6)
    @TestSecurity(user = "user", roles = {"ROLE_USER"})
    public void testValidateBalance_SufficientFunds() {
        if (secondAccountNumber != null) {
            given()
                    .contentType(ContentType.JSON)
                    .queryParam("amount", "500.00")
                    .when().post("/api/accounts/{accountNumber}/validate-balance", secondAccountNumber)
                    .then()
                    .statusCode(200)
                    .body("hasBalance", is(true))
                    .body("accountNumber", is(secondAccountNumber))
                    .body("currentBalance", is(500.0f));
        }
    }

    @Test
    @Order(7)
    @TestSecurity(user = "user", roles = {"ROLE_USER"})
    public void testValidateBalance_InsufficientFunds() {
        if (createdAccountNumber != null) {
            given()
                    .contentType(ContentType.JSON)
                    .queryParam("amount", "1500.00")
                    .when().post("/api/accounts/{accountNumber}/validate-balance", createdAccountNumber)
                    .then()
                    .statusCode(200)
                    .body("hasBalance", is(false))
                    .body("accountNumber", is(createdAccountNumber))
                    .body("currentBalance", is(0.0f));
        }
    }

    @Test
    @Order(8)
    @TestSecurity(user = "admin", roles = {"ROLE_ADMIN"})
    public void testDebitAccount() {
        if (secondAccountNumber != null) {
            String requestBody = """
                {
                    "amount": 100.00,
                    "transactionId": "TEST-DEBIT-001",
                    "description": "Test debit transaction"
                }
                """;

            given()
                    .contentType(ContentType.JSON)
                    .body(requestBody)
                    .when().post("/api/accounts/{accountNumber}/debit", secondAccountNumber)
                    .then()
                    .statusCode(200)
                    .body("newBalance", is(400.0f))
                    .body("transactionId", is("TEST-DEBIT-001"))
                    .body("accountNumber", is(secondAccountNumber));

            // Verify balance was updated
            given()
                    .when().get("/api/accounts/{accountNumber}/balance", secondAccountNumber)
                    .then()
                    .statusCode(200)
                    .body("balance", is(400.0f));
        }
    }

    @Test
    @Order(9)
    @TestSecurity(user = "admin", roles = {"ROLE_ADMIN"})
    public void testCreditAccount() {
        if (createdAccountNumber != null) {
            String requestBody = """
                {
                    "amount": 200.00,
                    "transactionId": "TEST-CREDIT-001",
                    "description": "Test credit transaction"
                }
                """;

            given()
                    .contentType(ContentType.JSON)
                    .body(requestBody)
                    .when().post("/api/accounts/{accountNumber}/credit", createdAccountNumber)
                    .then()
                    .statusCode(200)
                    .body("newBalance", is(200.0f))
                    .body("transactionId", is("TEST-CREDIT-001"))
                    .body("accountNumber", is(createdAccountNumber));

            // Verify balance was updated
            given()
                    .when().get("/api/accounts/{accountNumber}/balance", createdAccountNumber)
                    .then()
                    .statusCode(200)
                    .body("balance", is(200.0f));
        }
    }

    // Error condition tests
    @Test
    @Order(10)
    @TestSecurity(user = "user", roles = {"ROLE_USER"})
    public void testGetAccount_NotFound() {
        given()
                .when().get("/api/accounts/999999999")
                .then()
                .statusCode(404);
    }

    @Test
    @Order(11)
    @TestSecurity(user = "user", roles = {"ROLE_USER"})
    public void testValidateBalance_InvalidAmount() {
        if (createdAccountNumber != null) {
            given()
                    .contentType(ContentType.JSON)
                    .queryParam("amount", "-100.00")
                    .when().post("/api/accounts/{accountNumber}/validate-balance", createdAccountNumber)
                    .then()
                    .statusCode(400); // Bad Request
        }
    }

    @Test
    @Order(12)
    @TestSecurity(user = "admin", roles = {"ROLE_ADMIN"})
    public void testAccount_InsufficientFunds() {
        if (createdAccountNumber != null) {
            String requestBody = """
                {
                    "amount": 2000.00,
                    "transactionId": "TEST-DEBIT-FAIL",
                    "description": "Test insufficient funds"
                }
                """;

            given()
                    .contentType(ContentType.JSON)
                    .body(requestBody)
                    .when().post("/api/accounts/{accountNumber}/debit", createdAccountNumber)
                    .then()
                    .statusCode(400); // Bad Request - Insufficient funds
        }
    }

    @Test
    @Order(13)
    @TestSecurity(user = "admin", roles = {"ROLE_ADMIN"})
    public void testDebitInactiveAccount_ShouldFail() {
        if (secondAccountNumber != null) {
            String requestBody = """
                {
                    "amount": 50.00,
                    "transactionId": "TEST-INACTIVE-DEBIT",
                    "description": "Test debit on inactive account"
                }
                """;

            given()
                    .contentType(ContentType.JSON)
                    .body(requestBody)
                    .when().post("/api/accounts/{accountNumber}/debit", "1234567890123459")
                    .then()
                    .statusCode(400); // Bad Request - Account inactive
        }
    }

    @Test
    @Order(14)
    @TestSecurity(user = "user", roles = {"ROLE_USER"})
    public void testValidateInactiveAccount_ShouldFail() {

            given()
                    .contentType(ContentType.JSON)
                    .queryParam("amount", "100.00")
                    .when().post("/api/accounts/104/validate-balance")
                    .then()
                    .statusCode(400); // Bad Request - Account inactive

    }

    // Health and observability tests
    @Test
    public void testHealthEndpoint() {
        given()
                .when().get("/q/health")
                .then()
                .statusCode(200)
                .body("status", is("UP"));
    }

    @Test
    public void testMetricsEndpoint() {
        given()
                .when().get("/q/metrics")
                .then()
                .statusCode(200);
    }

    @Test
    public void testTransactionEventReceived() {
        InMemoryConnector.switchIncomingChannelsToInMemory("transactionss");
        InMemorySource<String> input = connectorIntegration.source("transactionss");
        String eventJson = String.format(
                "{\"eventType\":\"TRANSACTION_COMPLETED\", \"originAccount\":\"%s\", \"destinationAccount\":\"%s\", \"amount\":\"%s\", \"transactionId\":\"tx-123\"}",
                "1234567890123456", "1234567890123457", "5");
        input.send(eventJson);
        await().atLeast(5, TimeUnit.SECONDS);
    }
}
