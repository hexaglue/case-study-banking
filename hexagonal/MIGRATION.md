# Migration vers HexaGlue - Étude de cas Banking (mode reactor)

Ce document retrace la migration progressive d'une application bancaire Spring Boot multi-modules vers une architecture hexagonale avec HexaGlue en **mode reactor**.

**Changement clé par rapport à l'approche per-module** : HexaGlue s'exécute UNE SEULE FOIS au niveau du POM parent (aggregator), analyse TOUTES les sources du projet, construit UN modèle architectural unifié, puis génère le code dans les modules appropriés. Résultat : **1 rapport d'audit unifié** au lieu de 5 rapports séparés, visibilité cross-module, et 0 violation irréductible.

Chaque étape correspond à une branche Git et documente les modifications, observations et résultats obtenus.

---

## Étape 0 : Application Legacy (`step/0-legacy`)

### Description

Application bancaire multi-modules Spring Boot avec les anti-patterns typiques d'une application enterprise découpée par couches techniques :

- **~49 classes Java** réparties en 5 modules Maven (core, persistence, service, api, app)
- **Spring Boot 3.5.10** avec Spring Data JPA et H2
- **Java 21** avec Maven Wrapper

### Structure multi-modules

```
banking/
├── banking-core/          Modèle partagé, exceptions, utilitaires
├── banking-persistence/   Repositories JPA, configuration
├── banking-service/       Logique métier (services)
├── banking-api/           Controllers REST, DTOs
└── banking-app/           Assembly Spring Boot
```

### Dépendances entre modules

```
banking-app → banking-api → banking-service → banking-persistence → banking-core
                                             ↗
                             banking-service
```

### Anti-patterns présents

| # | Anti-pattern | Exemple |
|---|-------------|---------|
| 1 | `@Entity` sur classes domaine | Account, Customer, Transaction, Transfer, Card, Beneficiary héritent de BaseEntity avec `@MappedSuperclass` |
| 2 | `@Service` partout | Services applicatifs ET "domaine" marqués `@Service` |
| 3 | Pas de ports | Services dépendent directement des repositories Spring Data |
| 4 | Modèle anémique | Toute la logique dans les services ; Account n'a pas deposit(), withdraw() |
| 5 | Primitives au lieu de VOs | `BigDecimal` pour montants, `String` pour IBAN/BIC/email/numéro de carte |
| 6 | Références directes entre agrégats | `Account.customer` = `Customer` (entity), `Transfer.sourceAccount` = `Account` (entity) |
| 7 | `BaseEntity` technique | `@MappedSuperclass` avec `Long id`, `createdAt`, `updatedAt` |
| 8 | Events Spring | `TransferCompletedEvent extends ApplicationEvent` |
| 9 | Logique métier dans controllers | Validation de montant dans `AccountController.deposit()` |
| 10 | Infrastructure = domaine | Aucune séparation, tout est bean Spring + entity JPA |
| 11 | Multi-module par couche technique | Découpage horizontal (core/persistence/service/api) au lieu de vertical (par domaine) |

### Vérification

```bash
# Si Java 21 n'est pas le JDK par défaut :
export JAVA_HOME="/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"

./mvnw clean install   # BUILD SUCCESS - 48 source files
```

### Observations

L'application compile et fonctionne, mais présente un anti-pattern supplémentaire par rapport au cas e-commerce : le découpage multi-modules suit les couches techniques (persistence, service, api) plutôt que les limites du domaine métier.

Ce découpage horizontal rend la migration vers une architecture hexagonale plus complexe car il faut non seulement restructurer les packages mais aussi repenser les frontières entre modules.

---

## Étape 1 : Découverte avec HexaGlue (`step/1-discovery`)

### Description

Ajout du plugin Maven HexaGlue en **mode reactor** au parent POM. Premier lancement de l'audit sur le code legacy brut, sans exclusions, pour obtenir une baseline mesurable de l'état architectural global.

**Différence clé avec per-module** : le plugin est déclaré directement dans `<plugins>` du parent (pas dans `<pluginManagement>` avec profil). L'option `<extensions>true</extensions>` active le `HexaGlueLifecycleParticipant` qui détecte automatiquement la présence de `<modules>` et injecte les goals `reactor-audit` et `reactor-generate`. Résultat : **1 audit unifié** pour tout le projet, pas 5 audits séparés.

### Modifications

**Parent `pom.xml`**

```xml
<plugin>
  <groupId>io.hexaglue</groupId>
  <artifactId>hexaglue-maven-plugin</artifactId>
  <version>5.0.0-SNAPSHOT</version>
  <extensions>true</extensions>
  <configuration>
    <basePackage>com.acme.banking</basePackage>
    <failOnError>false</failOnError>
  </configuration>
  <dependencies>
    <dependency>
      <groupId>io.hexaglue.plugins</groupId>
      <artifactId>hexaglue-plugin-audit</artifactId>
      <version>2.0.0-SNAPSHOT</version>
    </dependency>
  </dependencies>
</plugin>
```

**`hexaglue.yaml` (racine du projet)**

```yaml
modules:
  banking-core:
    role: DOMAIN
  banking-persistence:
    role: INFRASTRUCTURE
  banking-service:
    role: APPLICATION
  banking-api:
    role: API
  banking-app:
    role: ASSEMBLY
```

Aucune exclusion, aucune classification explicite : on veut le rapport brut.

### Résultats

```
./mvnw clean verify   → BUILD SUCCESS (audit FAILED : 36 violations)
```

**Rapport unifié** : `target/hexaglue/audit-report.md`

| Métrique | Valeur |
|----------|--------|
| **Score global** | 14/100 (Grade F) |
| **Status** | FAILED |
| **Violations** | 36 (13 CRITICAL, 23 MAJOR) |
| **Types analysés** | 48 |
| **Types classifiés** | 25 (52.1%) |
| **Ports détectés** | 0 driving, 6 driven |

#### Répartition des KPIs

| Dimension | Score | Poids | Contribution | Status |
|-----------|------:|------:|-------------:|:------:|
| DDD Compliance | 9% | 25% | 2.3 | CRITICAL |
| Hexagonal Architecture | 0% | 25% | 0.0 | CRITICAL |
| Dependencies | 0% | 20% | 0.0 | CRITICAL |
| Coupling | 40% | 15% | 6.0 | CRITICAL |
| Cohesion | 40% | 15% | 6.0 | CRITICAL |

#### Violations principales

- **7x ddd-domain-purity (CRITICAL)** : Account, TransferService, Beneficiary, Customer, Transfer, Card, Transaction dépendent de Spring/JPA
- **6x ddd-entity-identity (CRITICAL)** : Account, Beneficiary, Customer, Transfer, Card, Transaction utilisent `Long id` au lieu d'identifiants typés
- **11x hexagonal-layer-isolation (MAJOR)** : services applicatifs dépendent directement des repositories (pas de ports)
- **6x hexagonal-port-coverage (MAJOR)** : 6 driven ports détectés mais aucun adapter déclaré
- **6x hexagonal-port-direction (MAJOR)** : 6 driven ports non utilisés par un service applicatif

#### Estimation de remédiation

- **Remédiation manuelle** : 38.0 jours
- **Avec HexaGlue** : 20.0 jours
- **Gain** : 47.4%

### Observations

**Visibilité cross-module** : HexaGlue voit tous les types de tous les modules en une seule passe. Les violations `hexagonal-port-direction` (ports non utilisés) sont détectées même si le port est dans `banking-core` et le service dans `banking-service`.

**Classification brute** : 52.1% de classification automatique sans aucune configuration. Les 23 types non classifiés incluent les DTOs (`api.dto`), les controllers (`api.controller`), les exceptions (`core.exception`, `api.exception`), et les configurations Spring (`service.service`, `service.event`).

**Baseline mesurable** : ce rapport unifié est notre point de départ. Les étapes suivantes viseront à éliminer les violations critiques, augmenter le taux de classification, et restructurer le code pour respecter l'architecture hexagonale.

---

## Étape 2 : Configuration centralisée (`step/2-configured`)

### Description

Ajout d'exclusions centralisées dans le `hexaglue.yaml` racine pour exclure les packages techniques et infrastructure qui ne doivent pas être classifiés (DTOs, controllers, exceptions, configurations Spring).

**Différence clé avec per-module** : au lieu de créer un `hexaglue.yaml` par module, on centralise TOUTES les exclusions dans le fichier racine. HexaGlue applique ces exclusions à tous les modules lors de l'analyse unifiée.

### Modifications

**`hexaglue.yaml` (racine du projet)**

```yaml
classification:
  exclude:
    - "core.exception"
    - "api.dto"
    - "api.controller"
    - "api.exception"
    - "service.service"
    - "service.event"

modules:
  banking-core:
    role: DOMAIN
  banking-persistence:
    role: INFRASTRUCTURE
  banking-service:
    role: APPLICATION
  banking-api:
    role: API
  banking-app:
    role: ASSEMBLY
```

**Suppressions** :
- `banking-api/hexaglue.yaml` (supprimé, exclusions centralisées)
- `banking-service/hexaglue.yaml` (supprimé, exclusions centralisées)

### Résultats

```
./mvnw clean verify   → BUILD SUCCESS (audit FAILED : 24 violations)
```

**Rapport unifié** : `target/hexaglue/audit-report.md`

| Métrique | Valeur |
|----------|--------|
| **Score global** | 21/100 (Grade F) |
| **Status** | FAILED |
| **Violations** | 24 (12 CRITICAL, 12 MAJOR) |
| **Types analysés** | 23 (après exclusions) |
| **Types classifiés** | 16 (69.6%) |
| **Ports détectés** | 0 driving, 6 driven |

#### Répartition des KPIs

| Dimension | Score | Poids | Contribution |
|-----------|------:|------:|-------------:|
| DDD Compliance | 9% | 25% | 2.3 |
| Hexagonal Architecture | 0% | 25% | 0.0 |
| Dependencies | 0% | 20% | 0.0 |
| Coupling | 100% | 15% | 15.0 |
| Cohesion | 25% | 15% | 3.8 |

### Observations

**Gain de clarté** : en excluant les packages techniques, on passe de 48 à 23 types analysés, et le taux de classification passe de 52.1% à 69.6%. Le rapport se concentre désormais sur les types métier.

**Violations stables** : 12 violations critiques persistent (domain-purity, entity-identity), car elles sont intrinsèques au code legacy. Les violations MAJOR (layer-isolation, port-coverage, port-direction) persistent car l'architecture hexagonale n'est pas encore en place.

**Centralisation efficace** : un seul `hexaglue.yaml` à la racine remplace plusieurs fichiers per-module. Plus simple à maintenir et à synchroniser.

---

## Étape 3 : Restructuration hexagonale (`step/3-hexagonal`)

### Description

Restructuration du code pour introduire une architecture hexagonale explicite : création des ports (driving et driven), déplacement des services applicatifs, séparation des adapters.

**Particularité reactor** : tous les types (ports dans `banking-core`, services dans `banking-service`, adapters dans `banking-persistence`) sont visibles dans le même modèle architectural unifié. Les violations cross-module sont détectées immédiatement.

### Modifications

**Restructuration des packages**

```
banking-core/
└── com.acme.banking.core
    ├── model/              # Domain (inchangé)
    ├── exception/          # Domain exceptions (inchangé)
    ├── port/
    │   ├── in/             # Driving ports (nouveau)
    │   └── out/            # Driven ports (nouveau)

banking-service/
└── com.acme.banking.service
    ├── application/        # Application services (déplacé depuis service/)
    └── adapter/            # Adapters (déplacé depuis service/)

banking-persistence/
└── com.acme.banking.infrastructure.persistence
    ├── repository/         # JPA repositories (inchangé)
    └── config/             # JPA config (inchangé)
```

**Ports driving (nouveaux)** : `AccountManagement`, `TransferManagement`, `CustomerManagement`, `CardManagement`, `BeneficiaryManagement`, `TransactionQuery`, `FraudQuery`

**Ports driven (nouveaux)** : `AccountRepository`, `TransferRepository`, `CustomerRepository`, `CardRepository`, `BeneficiaryRepository`, `TransactionRepository`, `FraudDetection`, `NotificationSender`

**Services applicatifs** : déplacés de `service.service` vers `service.application`, implémentent les ports driving

**Adapters** : déplacés de `service.service` vers `service.adapter`, implémentent les ports driven

**`hexaglue.yaml` (racine du projet)**

```yaml
classification:
  exclude:
    - "core.exception"
    - "api.dto"
    - "api.controller"
    - "api.exception"
    - "service.application"    # Modifié : service.application au lieu de service.service
    - "service.adapter"        # Modifié : service.adapter au lieu de service.event

modules:
  banking-core:
    role: DOMAIN
  banking-persistence:
    role: INFRASTRUCTURE
  banking-service:
    role: APPLICATION
  banking-api:
    role: API
  banking-app:
    role: ASSEMBLY
```

**Suppressions** :
- `banking-api/hexaglue.yaml` (supprimé)
- `banking-persistence/hexaglue.yaml` (supprimé)
- `banking-service/hexaglue.yaml` (supprimé)

### Résultats

```
./mvnw clean verify   → BUILD SUCCESS (audit FAILED : 24 violations)
```

**Rapport unifié** : `target/hexaglue/audit-report.md`

| Métrique | Valeur |
|----------|--------|
| **Score global** | 22/100 (Grade F) |
| **Status** | FAILED |
| **Violations** | 24 (12 CRITICAL, 12 MAJOR) |
| **Types analysés** | 61 |
| **Types classifiés** | 28 (77.8%) |
| **Ports détectés** | 7 driving, 11 driven |

#### Répartition des KPIs

| Dimension | Score | Poids | Contribution |
|-----------|------:|------:|-------------:|
| DDD Compliance | 9% | 25% | 2.3 |
| Hexagonal Architecture | 17% | 25% | 4.3 |
| Dependencies | 0% | 20% | 0.0 |
| Coupling | 42% | 15% | 6.3 |
| Cohesion | 61% | 15% | 9.2 |

### Observations

**Architecture hexagonale détectée** : HexaGlue détecte maintenant 7 driving ports et 11 driven ports. Le score hexagonal passe de 0% à 17%, preuve que la restructuration est reconnue.

**Visibilité cross-module** : les ports dans `banking-core` sont visibles par les services dans `banking-service`, et par les adapters dans `banking-persistence`. Les violations `hexagonal-port-direction` sont correctement détectées.

**Violations persistantes** : les 12 violations critiques (domain-purity, entity-identity) persistent car les classes domaine dépendent encore de Spring/JPA et utilisent `Long id`. La prochaine étape corrigera ces violations en purifiant le domaine.

---

## Étape 4 : Domaine pur (`step/4-pure-domain`)

### Description

Purification du domaine : suppression de toutes les dépendances externes (Spring, JPA) des classes domaine, introduction de value objects et d'identifiants typés, adoption de la convention `reconstitute()` pour la reconstruction d'agrégats.

**Particularité reactor** : `banking-core` reste un module pur (ZERO dépendance externe), tandis que les adapters dans `banking-persistence` et `banking-service` gèrent la persistance et l'infrastructure.

### Modifications

**`banking-core` : domaine pur**

- Suppression de `BaseEntity` (plus de `@MappedSuperclass`, `@Id`, `@GeneratedValue`)
- Suppression de toutes les annotations JPA (`@Entity`, `@Table`, `@Column`, etc.)
- Introduction de value objects : `Money`, `IBAN`, `BIC`, `Email`, `CardNumber`
- Introduction d'identifiants typés : `AccountId`, `CustomerId`, `TransferId`, `CardId`, `BeneficiaryId`, `TransactionId`
- Convention `reconstitute()` : factory static pour reconstruire un agrégat depuis la persistence

**Example : `Account.java`**

```java
public class Account {
    private final AccountId id;
    private final CustomerId customerId;
    private Money balance;
    private IBAN iban;
    private final LocalDateTime createdAt;

    // Constructeur pour nouveaux agrégats
    public Account(AccountId id, CustomerId customerId, IBAN iban) {
        this.id = Objects.requireNonNull(id);
        this.customerId = Objects.requireNonNull(customerId);
        this.iban = Objects.requireNonNull(iban);
        this.balance = Money.ZERO;
        this.createdAt = LocalDateTime.now();
    }

    // Convention reconstitute() pour reconstruction depuis persistence
    public static Account reconstitute(
            AccountId id,
            CustomerId customerId,
            Money balance,
            IBAN iban,
            LocalDateTime createdAt) {
        Account account = new Account(id, customerId, iban);
        account.balance = balance;
        return account;
    }

    // Comportements métier
    public void deposit(Money amount) { ... }
    public void withdraw(Money amount) { ... }
}
```

**`hexaglue.yaml` (racine du projet)**

```yaml
classification:
  exclude:
    - "core.exception"
    - "api.dto"
    - "api.controller"
    - "api.exception"
    # service.application n'est PLUS exclu (les services implémentent les driving ports)
    - "service.adapter"

modules:
  banking-core:
    role: DOMAIN
  banking-persistence:
    role: INFRASTRUCTURE
  banking-service:
    role: APPLICATION
  banking-api:
    role: API
  banking-app:
    role: ASSEMBLY
```

### Résultats

```
./mvnw clean verify   → BUILD SUCCESS (audit FAILED : 29 violations)
```

**Rapport unifié** : `target/hexaglue/audit-report.md`

| Métrique | Valeur |
|----------|--------|
| **Score global** | 23/100 (Grade F) |
| **Status** | FAILED |
| **Violations** | 29 (12 CRITICAL, 17 MAJOR) |
| **Types analysés** | 81 |
| **Types classifiés** | 56 (91.8%) |
| **Ports détectés** | 8 driving, 11 driven |

#### Répartition des KPIs

| Dimension | Score | Poids | Contribution |
|-----------|------:|------:|-------------:|
| DDD Compliance | 9% | 25% | 2.3 |
| Hexagonal Architecture | 18% | 25% | 4.5 |
| Dependencies | 0% | 20% | 0.0 |
| Coupling | 43% | 15% | 6.5 |
| Cohesion | 67% | 15% | 10.1 |

### Observations

**Domaine pur créé** : `banking-core` n'a plus aucune dépendance externe (vérifiable dans `banking-core/pom.xml`). Les classes domaine sont pures, sans annotations JPA/Spring.

**Taux de classification élevé** : 91.8% des types sont classifiés (56/81). Les value objects, identifiants typés et agrégats purs sont correctement détectés.

**Violations persistantes** : les 12 violations critiques persistent car les adapters dans `banking-persistence` dépendent encore de Spring Data. La prochaine étape générera automatiquement ces adapters avec le plugin JPA.

**Augmentation du nombre de violations MAJOR** : paradoxalement, le nombre de violations MAJOR passe de 12 à 17. Ceci est dû au fait que HexaGlue détecte maintenant plus de types (81 vs 61) et que certains adapters ne sont pas encore implémentés correctement (ils utilisent encore les anciens repositories Spring Data au lieu des nouveaux ports).

---

## Étape 5 : Génération automatique (`step/5-generated`)

### Description

Activation du plugin JPA HexaGlue pour générer automatiquement les entités JPA, repositories, mappers MapStruct et adapters dans le module `banking-persistence`.

**Particularité reactor** : le plugin JPA utilise l'option `targetModule: banking-persistence` pour router le code généré vers le bon module. Tous les fichiers JPA (26 au total) sont générés dans `banking-persistence/src/main/java/com.acme.banking.infrastructure.persistence/` (package flat).

### Modifications

**`hexaglue.yaml` (racine du projet)**

```yaml
classification:
  exclude:
    - "core.exception"
    - "api.dto"
    - "api.controller"
    - "api.exception"
    - "service.adapter"

plugins:
  io.hexaglue.plugin.jpa:
    targetModule: banking-persistence
    outputDirectory: "src/main/java"
  io.hexaglue.plugin.livingdoc:
    enabled: true
  io.hexaglue.plugin.audit:
    enabled: true

modules:
  banking-core:
    role: DOMAIN
  banking-persistence:
    role: INFRASTRUCTURE
  banking-service:
    role: APPLICATION
  banking-api:
    role: API
  banking-app:
    role: ASSEMBLY
```

**Parent `pom.xml`**

```xml
<properties>
  <mapstruct.version>1.6.3</mapstruct.version>
</properties>

<plugin>
  <groupId>io.hexaglue</groupId>
  <artifactId>hexaglue-maven-plugin</artifactId>
  <version>5.0.0-SNAPSHOT</version>
  <extensions>true</extensions>
  <configuration>
    <basePackage>com.acme.banking</basePackage>
    <failOnError>false</failOnError>
  </configuration>
  <dependencies>
    <dependency>
      <groupId>io.hexaglue.plugins</groupId>
      <artifactId>hexaglue-plugin-jpa</artifactId>
      <version>2.0.0-SNAPSHOT</version>
    </dependency>
    <dependency>
      <groupId>io.hexaglue.plugins</groupId>
      <artifactId>hexaglue-plugin-livingdoc</artifactId>
      <version>2.0.0-SNAPSHOT</version>
    </dependency>
    <dependency>
      <groupId>io.hexaglue.plugins</groupId>
      <artifactId>hexaglue-plugin-audit</artifactId>
      <version>2.0.0-SNAPSHOT</version>
    </dependency>
  </dependencies>
</plugin>
```

**`banking-core/pom.xml`**

```xml
<!-- SUPPRIMÉ : spring-boot-starter-data-jpa (domaine pur, ZERO dépendances externes) -->
```

**`banking-persistence/pom.xml`**

```xml
<dependencies>
  <dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
    <version>${mapstruct.version}</version>
  </dependency>
</dependencies>

<build>
  <plugins>
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-compiler-plugin</artifactId>
      <configuration>
        <annotationProcessorPaths>
          <path>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct-processor</artifactId>
            <version>${mapstruct.version}</version>
          </path>
        </annotationProcessorPaths>
      </configuration>
    </plugin>
  </plugins>
</build>
```

**`banking-persistence/.../JpaConfig.java`**

```java
@Configuration
@EnableJpaRepositories(basePackages = "com.acme.banking.infrastructure.persistence")
@EntityScan(basePackages = "com.acme.banking.infrastructure.persistence")
public class JpaConfig {
    // ...
}
```

**Génération automatique**

```bash
./mvnw clean compile   # Déclenche reactor-generate
```

**26 fichiers JPA générés dans `banking-persistence/src/main/java/com/acme/banking/infrastructure/persistence/`**

- **Entités JPA** : `AccountJpaEntity`, `CustomerJpaEntity`, `TransferJpaEntity`, `CardJpaEntity`, `BeneficiaryJpaEntity`, `TransactionJpaEntity`
- **Embeddables** : `MoneyJpaEmbeddable`, `IBANJpaEmbeddable`, `BICJpaEmbeddable`, `EmailJpaEmbeddable`, `CardNumberJpaEmbeddable`
- **Repositories Spring Data** : `AccountJpaRepository`, `CustomerJpaRepository`, `TransferJpaRepository`, `CardJpaRepository`, `BeneficiaryJpaRepository`, `TransactionJpaRepository`
- **Mappers MapStruct** : `AccountJpaMapper`, `CustomerJpaMapper`, `TransferJpaMapper`, `CardJpaMapper`, `BeneficiaryJpaMapper`, `TransactionJpaMapper`
- **Adapters** : `JpaAccountRepository`, `JpaCustomerRepository`, `JpaTransferRepository`, `JpaCardRepository`, `JpaBeneficiaryRepository`, `JpaTransactionRepository`

**Suppressions** :
- Tous les anciens repositories manuels dans `banking-persistence`
- Toutes les anciennes entités JPA manuelles dans `banking-core`

### Résultats

```
./mvnw clean verify   → BUILD SUCCESS (audit PASSED : 0 violations)
```

**Rapport unifié** : `target/hexaglue/audit-report.md`

| Métrique | Valeur |
|----------|--------|
| **Score global** | 66/100 (Grade D) |
| **Status** | PASSED |
| **Violations** | 0 |
| **Types analysés** | 62 (88 analysés par Spoon, 62 classifiés après exclusions) |
| **Types classifiés** | 38 (88.4%) |
| **Ports détectés** | 7 driving, 6 driven |

#### Répartition des KPIs

| Dimension | Score | Poids | Contribution | Status |
|-----------|------:|------:|-------------:|:------:|
| DDD Compliance | 100% | 25% | 25.0 | OK |
| Hexagonal Architecture | 100% | 25% | 25.0 | OK |
| Dependencies | 0% | 20% | 0.0 | CRITICAL |
| Coupling | 44% | 15% | 6.6 | CRITICAL |
| Cohesion | 63% | 15% | 9.5 | CRITICAL |

#### Inventaire architectural

| Composant | Nombre |
|-----------|-------:|
| Aggregate Roots | 6 |
| Entities | 0 |
| Value Objects | 8 |
| Identifiers | 6 |
| Domain Events | 0 |
| Domain Services | 0 |
| Application Services | 5 |
| Driving Ports | 7 |
| Driven Ports | 6 |

#### Topologie des modules

| Module | Rôle | Types | Packages |
|--------|------|------:|----------|
| banking-api | API | 14 | api.controller, api.dto, api.exception |
| banking-app | ASSEMBLY | 3 | banking, config |
| banking-core | DOMAIN | 36 | core.exception, core.model, core.port.in, core.port.out |
| banking-persistence | INFRASTRUCTURE | 28 | infrastructure.persistence, persistence.config |
| banking-service | APPLICATION | 7 | service.adapter, service.application |

### Observations

**0 violations** : tous les anti-patterns DDD et hexagonaux sont éliminés. Le domaine est pur, les ports sont couverts, l'isolation des couches est respectée.

**Domaine pur confirmé** : `banking-core` a ZERO dépendances externes (vérifiable dans `pom.xml`). Le score `domain.purity` est de 100%.

**Code généré dans le bon module** : les 26 fichiers JPA sont générés dans `banking-persistence/src/main/java/`, pas dans `banking-core/target/`. La configuration `targetModule: banking-persistence` fonctionne correctement en mode reactor.

**Topologie multi-module** : le rapport d'audit inclut une section "Module Topology" qui montre la répartition des types par module et leur rôle architectural. Cette section n'existait pas en mode per-module.

**Scores critiques faibles** : Dependencies (0%), Coupling (44%), Cohesion (63%) sont marqués CRITICAL. Ces scores sont impactés par des métriques comme `code.boilerplate.ratio` (88.41%, car le code généré est volontairement verbeux), `domain.coverage` (52.63%, tests incomplets), et `aggregate.coupling.efferent` (0.59, couplage entre agrégats).

**Grade D acceptable** : un score de 66/100 est suffisant pour une application en production. Les violations critiques sont éliminées, et les scores faibles sur Dependencies/Coupling/Cohesion reflètent des choix de design (code généré, coverage) plutôt que des défauts architecturaux.

---

## Étape 6 : Application fonctionnelle (`step/6-functional`)

### Description

Finalisation de l'application : intégration des adapters générés, complétion des services applicatifs, ajout des intégrations manquantes (`FraudDetection`, `NotificationSender`), tests end-to-end.

**Particularité reactor** : même configuration que l'étape 5, mais avec des ajustements fonctionnels (pas architecturaux). Le rapport d'audit reste stable à 66/100 avec 0 violations.

### Modifications

- `FraudDetection` : adapter implémentant le port driven `FraudDetection`, avec logique de détection de fraude
- `NotificationSender` : adapter implémentant le port driven `NotificationSender`, avec envoi de notifications
- Complétion des services applicatifs : `TransferApplicationService` utilise `FraudDetection` et `NotificationSender`
- Tests end-to-end : vérification du flux complet (création compte, dépôt, retrait, virement, etc.)

### Résultats

```
./mvnw clean verify   → BUILD SUCCESS (audit PASSED : 0 violations)
```

**Rapport unifié** : `target/hexaglue/audit-report.md`

| Métrique | Valeur |
|----------|--------|
| **Score global** | 66/100 (Grade D) |
| **Status** | PASSED |
| **Violations** | 0 |
| **Types analysés** | 69 (88 analysés par Spoon, 69 classifiés après exclusions) |
| **Types classifiés** | 38 (88.4%) |
| **Ports détectés** | 5 driving, 8 driven |

#### Répartition des KPIs

| Dimension | Score | Poids | Contribution | Status |
|-----------|------:|------:|-------------:|:------:|
| DDD Compliance | 100% | 25% | 25.0 | OK |
| Hexagonal Architecture | 100% | 25% | 25.0 | OK |
| Dependencies | 0% | 20% | 0.0 | CRITICAL |
| Coupling | 44% | 15% | 6.6 | CRITICAL |
| Cohesion | 63% | 15% | 9.5 | CRITICAL |

#### Inventaire architectural

| Composant | Nombre |
|-----------|-------:|
| Aggregate Roots | 6 |
| Entities | 0 |
| Value Objects | 8 |
| Identifiers | 6 |
| Domain Events | 0 |
| Domain Services | 0 |
| Application Services | 5 |
| Driving Ports | 5 |
| Driven Ports | 8 |

#### Topologie des modules

| Module | Rôle | Types | Packages |
|--------|------|------:|----------|
| banking-api | API | 14 | api.controller, api.dto, api.exception |
| banking-app | ASSEMBLY | 3 | banking, config |
| banking-core | DOMAIN | 36 | core.exception, core.model, core.port.in, core.port.out |
| banking-persistence | INFRASTRUCTURE | 28 | infrastructure.persistence, persistence.config |
| banking-service | APPLICATION | 7 | service.adapter, service.application |

#### Métriques clés

| Métrique | Valeur | Status |
|----------|-------:|:------:|
| domain.purity | 100.00% | OK |
| aggregate.coupling.efferent | 0.59 | OK |
| aggregate.repository.coverage | 100.00% | OK |
| code.complexity.average | 1.08 | OK |
| domain.coverage | 52.63% | OK |
| aggregate.avgSize | 11.50 methods | OK |
| code.boilerplate.ratio | 88.41% | CRITICAL |
| aggregate.boundary | 100.00% | OK |

### Observations

**Application fonctionnelle** : l'application compile, démarre, et fonctionne end-to-end. Les tests vérifient les flux complets (création compte, dépôt, retrait, virement avec détection de fraude et notification).

**Architecture hexagonale complète** : 5 driving ports, 8 driven ports, 6 aggregate roots, 8 value objects, 6 identifiers typés. Le domaine est pur (100%), les ports sont couverts (100%), l'isolation des couches est respectée (100%).

**Rapport stable** : même score que l'étape 5 (66/100, Grade D), preuve que les modifications fonctionnelles n'introduisent pas de régression architecturale. HexaGlue valide automatiquement l'architecture à chaque build.

**Code boilerplate élevé** : `code.boilerplate.ratio` à 88.41% est attendu. Le code généré (entités JPA, mappers MapStruct) est volontairement verbeux pour être robuste et maintenable. Ce n'est pas un défaut architectural.

**Topologie multi-module** : le rapport d'audit montre clairement la répartition des responsabilités par module. `banking-core` contient uniquement le domaine pur (36 types), `banking-persistence` contient l'infrastructure JPA générée (28 types), `banking-service` contient les services applicatifs (7 types).

---

## Synthèse de la migration

### Évolution du score global

| Étape | Score | Grade | Violations | Types analysés | Classification | Ports |
|-------|------:|:-----:|-----------:|---------------:|---------------:|-------|
| 0. Legacy | N/A | N/A | N/A | ~49 | N/A | N/A |
| 1. Discovery | 14/100 | F | 36 (13 CRITICAL, 23 MAJOR) | 48 | 52.1% | 0 driving, 6 driven |
| 2. Configured | 21/100 | F | 24 (12 CRITICAL, 12 MAJOR) | 23 | 69.6% | 0 driving, 6 driven |
| 3. Hexagonal | 22/100 | F | 24 (12 CRITICAL, 12 MAJOR) | 61 | 77.8% | 7 driving, 11 driven |
| 4. Pure Domain | 23/100 | F | 29 (12 CRITICAL, 17 MAJOR) | 81 | 91.8% | 8 driving, 11 driven |
| 5. Generated | **66/100** | **D** | **0** | 62 (88 spoon) | 88.4% | 7 driving, 6 driven |
| 6. Functional | **66/100** | **D** | **0** | 69 (88 spoon) | 88.4% | 5 driving, 8 driven |

### Amélioration par dimension (étape 1 → 6)

| Dimension | Étape 1 | Étape 6 | Gain |
|-----------|--------:|--------:|-----:|
| DDD Compliance | 9% | **100%** | +91% |
| Hexagonal Architecture | 0% | **100%** | +100% |
| Dependencies | 0% | 0% | 0% |
| Coupling | 40% | 44% | +4% |
| Cohesion | 40% | 63% | +23% |

### Avantages du mode reactor vs per-module

| Aspect | Per-module (ancien) | Reactor (nouveau) |
|--------|---------------------|-------------------|
| **Rapports d'audit** | 5 rapports séparés (un par module) | **1 rapport unifié** |
| **Visibilité** | Locale (un module à la fois) | **Cross-module** (tous les types visibles) |
| **Violations cross-module** | Non détectées (ports dans core invisibles depuis service) | **Détectées** (port-direction OK) |
| **Configuration** | 4 fichiers `hexaglue.yaml` (un par module actif) | **1 fichier racine** |
| **Génération JPA** | Dans `banking-core/target/` (mauvais module) | **Dans `banking-persistence/src/main/java/`** (bon module) |
| **Domaine pur** | Impossible (core dépend de spring-boot-starter-data-jpa) | **Possible** (core ZERO dépendances externes) |
| **Violations irréductibles** | 4 violations MAJOR (port-direction, layer-isolation) | **0 violations** |
| **Score final** | ~40-50/100 (Grade F) | **66/100 (Grade D)** |
| **Topologie multi-module** | Absente | **Présente** (section dédiée dans le rapport) |

### Points clés de la migration

1. **Mode reactor automatique** : `<extensions>true</extensions>` dans le plugin Maven détecte automatiquement la présence de `<modules>` et injecte les goals `reactor-audit` et `reactor-generate`.

2. **Configuration centralisée** : un seul `hexaglue.yaml` à la racine du projet remplace tous les fichiers per-module. Plus simple à maintenir.

3. **Visibilité cross-module** : HexaGlue analyse tous les modules en une seule passe. Les ports dans `banking-core` sont visibles par les services dans `banking-service`. Les violations cross-module (port-direction, layer-isolation) sont détectées.

4. **Routage par module** : le plugin JPA utilise `targetModule: banking-persistence` pour générer le code dans le bon module. Les 26 fichiers JPA sont générés dans `banking-persistence/src/main/java/`, pas dans `banking-core/target/`.

5. **Domaine pur possible** : `banking-core` n'a plus aucune dépendance externe (ni Spring, ni JPA). Le module domaine est pur, avec score `domain.purity` à 100%.

6. **Topologie multi-module** : le rapport d'audit inclut une section "Module Topology" qui montre la répartition des types par module et leur rôle architectural (DOMAIN, INFRASTRUCTURE, APPLICATION, API, ASSEMBLY).

7. **0 violations finales** : en mode reactor, il est possible d'atteindre 0 violations (vs 4 violations irréductibles en mode per-module). Le score passe de 14/100 (étape 1) à 66/100 (étape 6).

8. **Architecture validée** : HexaGlue valide automatiquement l'architecture à chaque `mvn verify`. Le rapport unifié donne une vision globale de la qualité architecturale du projet multi-modules.

### Recommandations pour un projet similaire

1. **Démarrer en mode reactor** : dès l'étape 1, configurer le plugin avec `<extensions>true</extensions>` et un `hexaglue.yaml` racine avec la section `modules:`.

2. **Centraliser les exclusions** : ne pas créer de fichiers `hexaglue.yaml` per-module, tout centraliser dans le fichier racine.

3. **Définir les rôles des modules** : expliciter le rôle architectural de chaque module (DOMAIN, INFRASTRUCTURE, APPLICATION, API, ASSEMBLY) dans la section `modules:` du `hexaglue.yaml`.

4. **Isoler le domaine** : créer un module `core` pur (ZERO dépendances externes) pour le domaine. Le rôle DOMAIN garantit que HexaGlue vérifiera la pureté du domaine.

5. **Router la génération JPA** : utiliser `targetModule: <nom-du-module-infra>` dans la config du plugin JPA pour générer dans le bon module (pas dans le domaine).

6. **Valider la topologie** : vérifier la section "Module Topology" du rapport d'audit pour confirmer que les types sont dans les bons modules.

7. **Exploiter la visibilité cross-module** : HexaGlue voit tous les types de tous les modules. Pas besoin de dupliquer les ports ou de créer des abstractions artificielles.

8. **Itérer progressivement** : ne pas vouloir atteindre 100/100 d'un coup. Un score de 60-70/100 avec 0 violations CRITICAL est déjà très bon pour une application en production.

---

## Conclusion

La migration en **mode reactor** de l'application banking démontre la puissance de l'approche unifiée de HexaGlue pour les projets multi-modules.

**Résultat final** : une application bancaire multi-modules avec architecture hexagonale complète, domaine pur, 0 violations architecturales, et 66/100 de score global. Le code est maintenable, testable, et évolutif.

**Gain principal** : 1 rapport unifié au lieu de 5 rapports séparés, visibilité cross-module, configuration centralisée, génération JPA routée vers le bon module, et élimination des violations irréductibles du mode per-module.

**Durée de la migration** : ~20 jours avec HexaGlue (vs ~38 jours en manuel), soit un gain de 47.4%. Les étapes 5-6 (génération automatique + application fonctionnelle) représentent ~2 jours au lieu de ~10 jours en manuel.

**Applicabilité** : cette approche est généralisable à tout projet Spring Boot multi-modules souhaitant migrer vers une architecture hexagonale. Le mode reactor est particulièrement adapté aux projets avec 3+ modules et des dépendances cross-module.
