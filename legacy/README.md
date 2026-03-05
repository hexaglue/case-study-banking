# Acme Banking - Legacy Application

Original banking application with a traditional layered architecture.
This is the starting point for the migration case study.

## Context

This project illustrates a common enterprise pattern: a Java application
organized by **technical layers** (core, persistence, service, api) instead
of domain boundaries.

The banking domain (accounts, transactions, transfers, cards) is rich in
value objects and domain events, making it an excellent candidate for
hexagonal architecture.

## Prerequisites

- **Java 21** (`brew install openjdk@21`)
- **Maven 3.9+**

If Java 21 is not your default JDK:

```bash
export JAVA_HOME="/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"
```

## Build and Run

```bash
# Build and install all modules
mvn clean install

# Start the application
mvn spring-boot:run -pl banking-app
```

The application starts on [http://localhost:8080](http://localhost:8080).

### Available endpoints

- **H2 Console**: [http://localhost:8080/h2-console](http://localhost:8080/h2-console)
  (JDBC URL: `jdbc:h2:mem:bankingdb`, user: `sa`, no password)

> **Note:** this legacy application does not include Swagger/OpenAPI documentation.

## End-to-End Testing

Once the application is running, test with `curl`:

```bash
# Create a customer
curl -s -X POST http://localhost:8080/api/customers \
  -H "Content-Type: application/json" \
  -d '{"firstName":"John","lastName":"Doe","email":"john@example.com","phone":"0612345678"}'

# Open an account for customer 1
curl -s -X POST http://localhost:8080/api/accounts \
  -H "Content-Type: application/json" \
  -d '{"customerId":1,"type":"CHECKING","initialDeposit":1000.00}'

# List all accounts
curl -s http://localhost:8080/api/accounts

# Deposit money
curl -s -X POST http://localhost:8080/api/accounts/1/deposit \
  -H "Content-Type: application/json" \
  -d '{"amount":500.00}'

# Withdraw money
curl -s -X POST http://localhost:8080/api/accounts/1/withdraw \
  -H "Content-Type: application/json" \
  -d '{"amount":200.00}'

# Get account details
curl -s http://localhost:8080/api/accounts/1

# Create a second customer + account for transfers
curl -s -X POST http://localhost:8080/api/customers \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Jane","lastName":"Smith","email":"jane@example.com","phone":"0698765432"}'

curl -s -X POST http://localhost:8080/api/accounts \
  -H "Content-Type: application/json" \
  -d '{"customerId":2,"type":"CHECKING","initialDeposit":500.00}'

# Initiate a transfer
curl -s -X POST http://localhost:8080/api/transfers \
  -H "Content-Type: application/json" \
  -d '{"sourceAccountId":1,"destinationAccountId":2,"amount":100.00,"description":"Test transfer"}'

# Execute the transfer
curl -s -X POST http://localhost:8080/api/transfers/1/execute

# Get transaction history
curl -s http://localhost:8080/api/transactions/account/1

# Issue a card
curl -s -X POST http://localhost:8080/api/cards \
  -H "Content-Type: application/json" \
  -d '{"accountId":1,"cardNumber":"4111111111111111","expiryDate":"12/28","cvv":"123"}'

# Get cards for an account
curl -s http://localhost:8080/api/cards/account/1
```

## Project Structure

```
legacy/
├── banking-core/          Shared model, exceptions, utilities
├── banking-persistence/   JPA repositories, configuration
├── banking-service/       Business logic (@Service)
├── banking-api/           REST controllers, DTOs
└── banking-app/           Spring Boot assembly
```

### Module Dependencies

```
banking-app -> banking-api -> banking-service -> banking-persistence -> banking-core
```

## Documented Anti-Patterns

This legacy application intentionally illustrates 11 common anti-patterns:

1. **Anemic model**: entities are plain data containers with no business logic
2. **JPA coupling**: `@Entity` and `@MappedSuperclass` on domain classes
3. **No ports**: services depend directly on Spring Data repositories
4. **Primitive obsession**: `String` for IBAN, BIC, email; `BigDecimal` for amounts
5. **Horizontal layering**: Maven modules organized by technical layer

See [MIGRATION.md](../MIGRATION.md) for the complete list and migration steps.

## REST API

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/accounts` | List accounts |
| `POST` | `/api/accounts` | Open an account |
| `GET` | `/api/accounts/{id}` | Get account details |
| `POST` | `/api/accounts/{id}/deposit` | Deposit money |
| `POST` | `/api/accounts/{id}/withdraw` | Withdraw money |
| `DELETE` | `/api/accounts/{id}` | Close an account |
| `POST` | `/api/customers` | Create a customer |
| `GET` | `/api/customers/{id}` | Get customer details |
| `PUT` | `/api/customers/{id}` | Update a customer |
| `POST` | `/api/transfers` | Initiate a transfer |
| `POST` | `/api/transfers/{id}/execute` | Execute a transfer |
| `POST` | `/api/transfers/{id}/cancel` | Cancel a transfer |
| `GET` | `/api/transfers/{id}` | Get transfer details |
| `GET` | `/api/transactions/account/{id}` | Transaction history |
| `GET` | `/api/transactions/account/{id}/statement` | Account statement |
| `POST` | `/api/cards` | Issue a card |
| `POST` | `/api/cards/{id}/block` | Block a card |
| `POST` | `/api/cards/{id}/activate` | Activate a card |
| `GET` | `/api/cards/account/{id}` | Cards for an account |
