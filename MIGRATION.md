# Migration Guide: Legacy to Hexagonal Architecture

This document describes the migration of a Spring Boot banking application from a classic layered architecture to a hexagonal architecture with code generation by [HexaGlue](https://hexaglue.io).

**Technologies**: Spring Boot 3.5.10, Java 21, HexaGlue 6.1.0, MapStruct 1.6.3

**Structure**: the repository contains two distinct codebases:
- `legacy/`: original application (layered architecture, Spring Boot monolith)
- `hexagonal/`: migrated application (hexagonal architecture, multi-module, generated code)

---

## 1. Starting Point: Legacy Application

The legacy application is a classic Spring Boot monolith with 5 Maven modules:

| Module | Role |
|--------|------|
| `banking-core` | Domain model (JPA entities) |
| `banking-persistence` | Spring Data JPA repositories |
| `banking-service` | Business services |
| `banking-api` | REST controllers and DTOs |
| `banking-app` | Spring Boot bootstrap |

### Inventory

- **48 Java files** in total
- **6 JPA entities**: `Account`, `Customer`, `Transaction`, `Transfer`, `Card`, `Beneficiary`
- All extend `BaseEntity` (`@MappedSuperclass` with `@Id`, `@PrePersist`, `@PreUpdate`)
- **0 value objects**, **0 typed identifiers**, **0 ports** (driving or driven)
- **7 services** in `banking-service` (including `FraudDetectionClient` and `NotificationService`)
- **5 REST controllers**, **8 DTOs** (records)
- **6 Spring Data JPA repositories**
- `banking-core` depends on `spring-boot-starter-data-jpa`

### Identified Anti-Patterns (11)

| # | Anti-Pattern | Example |
|---|-------------|---------|
| 1 | **Domain contaminated by JPA** | `@Entity`, `@Table`, `@ManyToOne` directly on domain classes |
| 2 | **Anemic domain model** | All business logic in services (`AccountService.deposit()`, `TransferService.executeTransfer()`), entities are pure data holders |
| 3 | **Aggregate boundary violations** | Direct entity references (`Account → Customer`, `Transfer → Account`) via `@ManyToOne` |
| 4 | **No ports** | No abstraction interfaces, repositories injected directly into services |
| 5 | **Framework coupling** | `TransferCompletedEvent` extends Spring `ApplicationEvent`, `@Transactional` everywhere |
| 6 | **No value objects** | IBAN = `String`, amounts = raw `BigDecimal`, address = 4 `String` fields in `Customer` |
| 7 | **Utility classes in domain** | `IbanUtils` and `MoneyUtils` in `banking-core` instead of value objects |
| 8 | **Security concerns** | CVV stored as plain text in `Card`, card numbers unencrypted |
| 9 | **Duplicated logic** | `Transaction` creation duplicated across `AccountService`, `TransferService`, `TransactionService` |
| 10 | **Scattered validation** | `amount > 0` check in `AccountController` instead of domain |
| 11 | **No domain events** | Spring `ApplicationEvent` instead of explicit domain events |

### Critical dependency: `banking-core/pom.xml`

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
```

The domain module depends directly on JPA infrastructure: impossible to test or reuse without Spring.

---

## 2. Step 1: Hexagonal Restructuring

First step: introduce ports and reorganize packages to separate concerns.

### Driving ports (inbound)

5 interfaces created in `banking-core/src/main/java/com/acme/banking/core/port/in/`:

| Interface | Methods |
|-----------|---------|
| `AccountUseCases` | `openAccount`, `closeAccount`, `getBalance`, `getAccount`, `getAccountByNumber`, `getAccountsByCustomer`, `getAllAccounts`, `deposit`, `withdraw` |
| `CustomerUseCases` | `createCustomer`, `updateCustomer`, `getCustomer`, `getCustomerByEmail` |
| `TransferUseCases` | `initiateTransfer`, `executeTransfer`, `cancelTransfer`, `getTransfer`, `getTransfersByAccount` |
| `CardUseCases` | `issueCard`, `blockCard`, `activateCard`, `getCardsByAccount` |
| `TransactionUseCases` | `recordTransaction`, `getHistory`, `getStatement` |

### Driven ports (outbound)

8 interfaces created in `banking-core/src/main/java/com/acme/banking/core/port/out/`:

| Interface | Type | Methods |
|-----------|------|---------|
| `AccountRepository` | REPOSITORY | `save`, `findById`, `findByAccountNumber`, `findByCustomerId`, `findByActiveTrue`, `findAll` |
| `CustomerRepository` | REPOSITORY | `save`, `findById`, `findByEmail`, `existsByEmail` |
| `CardRepository` | REPOSITORY | `save`, `findById`, `findByAccountId`, `findByCardNumber` |
| `TransactionRepository` | REPOSITORY | `save`, `findByAccountId`, `findByAccountIdAndType`, `findByAccountIdOrderByCreatedAtDesc` |
| `TransferRepository` | REPOSITORY | `save`, `findById`, `findBySourceAccountId`, `findByTargetAccountId`, `findByStatus` |
| `BeneficiaryRepository` | REPOSITORY | `save`, `findByCustomerId` |
| `FraudDetection` | GATEWAY | `checkTransfer`, `checkTransaction` |
| `NotificationSender` | EVENT_PUBLISHER | `sendTransferNotification`, `sendCardAlert` |

### Application services

5 services moved to `banking-service/src/main/java/com/acme/banking/service/application/`:

- `AccountApplicationService` (implements `AccountUseCases`)
- `CustomerApplicationService` (implements `CustomerUseCases`)
- `TransferApplicationService` (implements `TransferUseCases`)
- `CardApplicationService` (implements `CardUseCases`)
- `TransactionApplicationService` (implements `TransactionUseCases`)

### Manual adapters

2 adapters created in `banking-service/src/main/java/com/acme/banking/service/adapter/out/`:

- `FraudDetectionAdapter` (`@Component`, implements `FraudDetection`): stub that logs and returns `true`
- `NotificationSenderAdapter` (`@Component`, implements `NotificationSender`): stub that logs

---

## 3. Step 2: Domain Purification

Goal: make `banking-core` completely independent of any infrastructure.

### Removing `BaseEntity`

- Removed the `BaseEntity` class and all JPA annotations (`@Entity`, `@Table`, `@Column`, `@ManyToOne`, `@OneToMany`, etc.)
- Each aggregate manages its own `id` via a typed identifier

### Introducing typed identifiers

6 records in `com.acme.banking.core.model`:

```java
public record AccountId(Long value) { /* non-null validation */ }
public record CustomerId(Long value) { /* ... */ }
public record CardId(Long value) { /* ... */ }
public record TransactionId(Long value) { /* ... */ }
public record TransferId(Long value) { /* ... */ }
public record BeneficiaryId(Long value) { /* ... */ }
```

### Introducing value objects

4 records in `com.acme.banking.core.model`:

| Value Object | Fields | Business Logic |
|-------------|--------|----------------|
| `Money` | `BigDecimal amount`, `String currency` | `add()`, `subtract()`, `negate()`, `isGreaterThanOrEqual()`, 2-decimal rounding |
| `Email` | `String value` | Non-blank validation, must contain `@` |
| `Address` | `street`, `city`, `zipCode`, `country` | Non-blank validation on each field |
| `Iban` | `String value` | Length 15-34 validation, ISO format, `formatted()` |

Existing enumerations (`AccountType`, `CardStatus`, `TransactionType`, `TransferStatus`) are kept as-is.

### Enriching aggregates

Direct entity references replaced with typed identifiers, business logic added:

| Aggregate | Factory Methods | Business Logic |
|-----------|----------------|----------------|
| `Account` | `open()`, `reconstitute()` | `deposit(Money)`, `withdraw(Money)`, `close()` |
| `Customer` | `create()`, `reconstitute()` | `updateProfile()`, `updateAddress()`, `getFullName()` |
| `Transfer` | `initiate()`, `reconstitute()` | `execute()`, `cancel()` (state machine: PENDING → COMPLETED/CANCELLED) |
| `Card` | `issue()`, `reconstitute()` | `block()`, `activate()`, `getMaskedNumber()` |
| `Transaction` | `create()`, `reconstitute()` | Immutable after creation |
| `Beneficiary` | `create()`, `reconstitute()` | — |

The `reconstitute()` convention is present on every aggregate: it is the factory method used by generated mappers to rebuild a domain object from persistence, without applying business validation rules.

### Result: `banking-core/pom.xml`

```xml
<dependency>
    <groupId>org.jmolecules</groupId>
    <artifactId>jmolecules-hexagonal-architecture</artifactId>
    <version>2.0.1</version>
</dependency>
```

Only dependency: jMolecules for `@SecondaryPort` annotations (architectural markers for HexaGlue analysis). **Zero Spring dependencies, zero JPA dependencies**.

---

## 4. Step 3: JPA Generation with HexaGlue

HexaGlue analyzes domain code at compile time and automatically generates the persistence layer.

### `hexaglue.yaml` configuration (excerpt)

```yaml
classification:
  exclude:
    - "com.acme.banking.core.exception.*"

plugins:
  io.hexaglue.plugin.jpa:
    entitySuffix: "JpaEntity"
    repositorySuffix: "JpaRepository"
    adapterSuffix: "RepositoryAdapter"
    generateRepositories: true
    generateAdapters: true
    generateEmbeddables: true
    enableAuditing: false
    targetModule: banking-persistence
    outputDirectory: "src/main/java"

modules:
  banking-core:
    role: DOMAIN
  banking-persistence:
    role: INFRASTRUCTURE
```

### Files generated in `banking-persistence` (26 files)

| Type | Count | Examples |
|------|-------|----------|
| JPA Entities | 6 | `AccountJpaEntity`, `CustomerJpaEntity`, `TransferJpaEntity`, ... |
| JPA Repositories | 6 | `AccountJpaRepository`, `CustomerJpaRepository`, ... |
| MapStruct Mappers | 6 | `AccountMapper`, `CustomerMapper`, ... |
| Repository Adapters | 6 | `AccountRepositoryAdapter`, `CustomerRepositoryAdapter`, ... |
| Embeddables | 2 | `MoneyEmbeddable`, `AddressEmbeddable` |

Each `RepositoryAdapter` implements the corresponding domain driven port. It delegates to the generated `JpaRepository` and uses the MapStruct `Mapper` for domain-to-JPA conversion.

Mappers use the `reconstitute()` convention to rebuild domain objects.

---

## 5. Step 4: REST Generation with HexaGlue

HexaGlue generates REST controllers and DTOs from driving ports.

### `hexaglue.yaml` configuration (excerpt)

```yaml
plugins:
  io.hexaglue.plugin.rest:
    targetModule: banking-api
    outputDirectory: "src/main/java"
    flattenValueObjects: true
    generateExceptionHandler: true
    generateConfiguration: false

modules:
  banking-api:
    role: API
```

- `flattenValueObjects: true`: nested value objects are flattened in DTOs (e.g., `Money` becomes `balanceAmount` + `balanceCurrency` fields in `AccountResponse`)
- `generateConfiguration: false`: Spring configuration is manual in `banking-app` (the `ApplicationService` classes are in a different module)

### Files generated in `banking-api` (24 files)

| Type | Count | Examples |
|------|-------|----------|
| Controllers | 5 | `AccountController`, `CustomerController`, `TransferController`, `CardController`, `TransactionController` |
| Request DTOs | 13 | `OpenAccountRequest`, `DepositRequest`, `WithdrawRequest`, `CreateCustomerRequest`, `InitiateTransferRequest`, `IssueCardRequest`, ... |
| Response DTOs | 6 | `AccountResponse`, `CustomerResponse`, `TransferResponse`, `CardResponse`, `TransactionResponse`, `MoneyResponse` |
| Exception Handler | 1 | `GlobalExceptionHandler` |

Generated controllers automatically include:
- Swagger/OpenAPI annotations (`@Operation`, `@ApiResponse`) on every endpoint
- Bean Validation (`@Valid`, `@NotNull`, `@NotBlank`) on request DTOs
- `from()` factory methods on response DTOs for domain-to-DTO conversion
- Standard HTTP status codes (201 for creation, 204 for deletion, etc.)

---

## 6. Step 5: Running Application

### Assembly in `banking-app`

The `banking-app` module wires all components together via `ApplicationServiceConfig` (`@Configuration`):

```java
@Bean
public AccountUseCases accountUseCases(
        AccountRepository acctRepo,
        CustomerRepository custRepo,
        TransactionRepository txRepo) {
    return new AccountApplicationService(acctRepo, custRepo, txRepo);
}

@Bean
public TransferUseCases transferUseCases(
        TransferRepository transferRepo,
        AccountRepository acctRepo,
        TransactionRepository txRepo,
        FraudDetection fraudDetection,
        NotificationSender notificationSender) {
    return new TransferApplicationService(
            transferRepo, acctRepo, txRepo, fraudDetection, notificationSender);
}

// ... same pattern for CustomerUseCases, CardUseCases, TransactionUseCases
```

Repositories are autowired from generated adapters (`@Component`). Gateways (`FraudDetectionAdapter`, `NotificationSenderAdapter`) are also autowired.

### Spring Boot configuration (`application.yml`)

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:bankingdb
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
  h2:
    console:
      enabled: true
      path: /h2-console

server:
  port: 8080
```

### JPA configuration (`JpaConfig`)

```java
@Configuration
@EnableJpaRepositories(basePackages = "com.acme.banking.infrastructure.persistence")
@EntityScan(basePackages = "com.acme.banking.infrastructure.persistence")
public class JpaConfig {}
```

The scan explicitly targets the generated persistence package.

---

## 7. Migration Summary

### Factual Comparison

| Metric | Legacy | Hexagonal |
|--------|--------|-----------|
| Java files | 48 | 98 (50 generated) |
| Maven modules | 5 | 5 |
| Driving ports | 0 | 5 |
| Driven ports | 0 | 8 |
| Rich aggregates | 0 (anemic) | 6 (with business logic) |
| Typed identifiers | 0 (`Long` everywhere) | 6 records |
| Value objects | 0 (`String`, `BigDecimal`) | 4 records + 4 enums |
| `banking-core` dependencies | `spring-boot-starter-data-jpa` | `jmolecules-hexagonal-architecture` |
| Hand-written files | 48 | 48 |
| HexaGlue-generated files | 0 | 50 |

### HexaGlue Audit Report

The report is generated by `mvn clean verify` (goal `reactor-audit`).

**Verdict**: 67/100, Grade D, **PASSED** (0 violations, 0 issues)

| Dimension | Score | Threshold | Status |
|-----------|------:|----------:|:------:|
| DDD Compliance | 100% | 90% | OK |
| Hexagonal Architecture | 100% | 90% | OK |
| Dependencies | 0% | 80% | CRITICAL |
| Coupling | 51% | 70% | CRITICAL |
| Cohesion | 68% | 80% | WARNING |

**Strengths**:
- DDD Compliance 100%: all aggregates, value objects, and identifiers are correctly classified
- Hexagonal Architecture 100%: all ports have adapters, driving/driven separation respected
- Domain Purity 100%: no infrastructure dependencies in the domain
- Repository Coverage 100%: every aggregate has a repository port and adapter
- 0 architectural violations

**Areas for improvement**:
- Dependencies 0%: the KPI measures inter-module dependencies declared in POMs, not ports
- Coupling 51%: average instability of 0.51 across 13 packages (9 problematic)
- Cohesion 68%: average relational cohesion H=0.49 (threshold min 1.5)
- Event Coverage 0%: no aggregate emits domain events

### Architectural Inventory (audit)

| Component | Count |
|-----------|------:|
| Aggregate Roots | 6 |
| Value Objects | 8 |
| Identifiers | 6 |
| Application Services | 5 |
| Driving Ports | 5 |
| Driven Ports | 8 |
| Analyzed types | 38 |
| Bounded Contexts | 1 |

### Module Topology

| Module | Role | Types |
|--------|------|------:|
| `banking-core` | DOMAIN | 36 |
| `banking-persistence` | INFRASTRUCTURE | 28 |
| `banking-api` | API | 24 |
| `banking-service` | APPLICATION | 7 |
| `banking-app` | ASSEMBLY | 3 |

---

## Quick Start

```bash
# Legacy
cd legacy && mvn clean spring-boot:run
# → http://localhost:8080

# Hexagonal
cd hexagonal
mvn clean install
java -jar banking-app/target/banking-app-0.1.0-SNAPSHOT.jar
# → http://localhost:8080
# → Swagger UI: http://localhost:8080/swagger-ui.html
# → H2 Console: http://localhost:8080/h2-console
```

> **Why `java -jar` instead of `mvn spring-boot:run`?** The `spring-boot:run` goal
> re-triggers the `test-compile` phase, which re-executes HexaGlue's `generate` goal in
> single-module mode. This interferes with classpath resolution and causes
> `ClassNotFoundException` errors. Using the fat JAR directly avoids this issue.
>
> **Why `install`?** The `install` phase includes all prior lifecycle phases (`compile`,
> `test`, `verify`), so HexaGlue code generation and audit run automatically. It also
> packages the Spring Boot fat JAR needed to start the application.

To regenerate code and the audit report without installing:

```bash
cd hexagonal
mvn clean verify
# Audit report: target/hexaglue/reports/audit/AUDIT-REPORT.md
# Living doc: target/hexaglue/reports/living-doc/
```
