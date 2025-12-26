package com.bank.account.integration;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;

import static io.restassured.RestAssured.given;

/**
  Security tests for HTTP endpoints in accounts microservice
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AccountSecurityTest {

    private static String createdAccountNumber;

    @Test
    public void testGetAccount_WithoutAuthentication() {
        if (createdAccountNumber != null) {
            given()
                    .when().get("/api/accounts/{accountNumber}", createdAccountNumber)
                    .then()
                    .statusCode(401); // Unauthorized
        }
    }

    @Test
    @TestSecurity(user = "viewer", roles = {"ROLE_VIEWER"})
    public void testCreateAccount_InsufficientRole() {
        String requestBody = """
            {
                "customerId": 1,
                "accountType": "CHECKING"
            }
            """;

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when().post("/api/accounts")
                .then()
                .statusCode(403); // Forbidden
    }

}
