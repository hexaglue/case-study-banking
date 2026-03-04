# Migration vers HexaGlue - Étude de cas Banking

Ce document retrace la migration progressive d'une application bancaire
Spring Boot multi-modules vers une architecture hexagonale avec HexaGlue.

Chaque étape correspond à une branche Git et documente les observations,
les problèmes rencontrés et les résultats obtenus.

---

## Étape 0 : Application Legacy (`step/0-legacy`)

### Description

Application bancaire multi-modules Spring Boot avec les anti-patterns
typiques d'une application enterprise découpée par couches techniques :

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

L'application compile et fonctionne, mais présente un anti-pattern supplémentaire
par rapport au cas e-commerce : le découpage multi-modules suit les couches techniques
(persistence, service, api) plutôt que les limites du domaine métier.

Ce découpage horizontal rend la migration vers une architecture hexagonale plus
complexe car il faut non seulement restructurer les packages mais aussi repenser
les frontières entre modules.

---

## Étape 1 : Découverte avec HexaGlue (`step/1-discovery`)

*À venir*

---

## Étape 2 : Configuration et exclusions (`step/2-configured`)

*À venir*

---

## Étape 3 : Restructuration hexagonale (`step/3-hexagonal`)

*À venir*

---

## Étape 4 : Purification du domaine (`step/4-pure-domain`)

*À venir*

---

## Étape 5 : Génération et audit (`step/5-generated`)

*À venir*

---

## Étape 6 : Application fonctionnelle (`main`)

*À venir*
