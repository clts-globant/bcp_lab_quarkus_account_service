# Account service

This project uses Quarkus.

This microservice creates and allows basic validation of accounts for the case uses described by the transactions
microservice: https://github.com/clts-globant/bcp_lab_quarkus_transaction_service

Features:

* Get account details

`
curl -v -X GET http://localhost:8082/api/accounts/{account_id}
-H "Authorization: Bearer $JWT_TOKEN"
`

* Get a customer's accounts

`
curl -v -X GET http://localhost:8082/api/accounts/customer/{customer_id} 
-H "Authorization: Bearer $JWT_TOKEN"
`

Details include accountNumber, related customer id, balance, account type (not relevant in this version),
status (`ACTIVE` to be ready for transactions), and timestamps of creation (to be deleted in the future).

* Create an account

`
curl -v -X POST http://localhost:8082/api/accounts
-H "Content-Type: application/json"
-H "Authorization: Bearer $JWT_ADMIN_TOKEN"
-d '{
"customerId": {valid_customer_id},
"accountType": "SAVINGS",
"initialBalance": 500.00
}'
`

`initialBalance` is optional.

* Check an account's balance

`
curl -v -X GET http://localhost:8082/api/accounts/{account_id}/balance
-H "Authorization: Bearer $JWT_TOKEN"
`

Similar to account details, but just balance + status.

* Check validity of an account for a transaction (status, balance, etc.)

`
curl -v -X POST "http://localhost:8082/api/accounts/acc-12345/validate-balance?amount={cash_to_be_deducted}"
-H "Authorization: Bearer $JWT_TOKEN"
`

The cash to be deducted uses dots for decimal/cents, e.g. 12.50

* Debit/deduct balance from an account

`
curl -v -X POST http://localhost:8082/api/accounts/{account_id}/debit
-H "Content-Type: application/json"
-H "Authorization: Bearer $JWT_ADMIN_TOKEN"
-d '{
"amount": 100.00,
"transactionId": "{unique_uuid}"
}'
`

The amount, as it can be seen, uses dots for decimal/cents precision. Don't use negatives.

* Credit/add balance to an account

`
curl -v -X POST http://localhost:8082/api/accounts/acc-12345/credit
-H "Content-Type: application/json"
-H "Authorization: Bearer $JWT_ADMIN_TOKEN"
-d '{
"amount": 250.25,
"transactionId": "txn-credit-xyz-456"
}'
`

The amount, as it can be seen, uses dots for decimal/cents precision. Don't use negative values.

Basic health checks (like `q/health`) and metrics are supported thanks to Quarkus/micrometer. 
Read https://quarkus.io/guides/management-interface-reference for more details.

## Unit/integration tests
Run
```shell script
./mvnw test
```

## Before running

Don't forget to boot up a PostgreSQL instance plus a Kafka cluster before starting this service.
Respective ports/URLs and kafka topic can be configured in `src/main/resources/application.yml`

## Running the application in dev mode

You can run your application in dev mode + live coding:
```shell script
./mvnw compile quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at http://localhost:8080/q/dev/.

## Packaging and running the application

The application can be packaged using:
```shell script
./mvnw package
```
Not an uber jar, as the dependencies are copied into the `target/quarkus-app/lib/` directory.

Run with `java -jar target/quarkus-app/quarkus-run.jar`.

For uber jar:
```shell script
./mvnw package -Dquarkus.package.type=uber-jar
```

Run with `java -jar target/*-runner.jar`.

## Creating a native executable
 
```shell script
./mvnw package -Dnative
```

Application not tested with native build, so far.

You can run the native executable build in a container with: 
```shell script
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

Then simply execute with: `./target/account-service-1.0.0-SNAPSHOT-runner`

## Generating a valid JWT

The JWT is only checked for completeness + the ROLE_ADMIN role/permission, not for full
authorization+authentication, as a full identity system wasn't implemented for the whole solution.
Otherwise, it should be usable.

Instructions to generate a key pair: https://techdocs.akamai.com/iot-token-access-control/docs/generate-rsa-keys
Instructions to generate a JWT wit said keys: https://techdocs.akamai.com/iot-token-access-control/docs/generate-jwt-rsa-key