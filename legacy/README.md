# Acme Banking - Étude de cas HexaGlue

Application bancaire Spring Boot multi-modules utilisée comme étude de cas
pour la migration progressive vers une architecture hexagonale avec
[HexaGlue](https://hexaglue.io).

## Contexte

Ce projet illustre un scénario courant en entreprise : une application Java
découpée en **modules Maven par couches techniques** (core, persistence, service, api)
plutôt que par limites de domaine métier.

Le domaine bancaire (comptes, transactions, virements, cartes) est riche en
value objects et domain events, ce qui en fait un excellent candidat pour
démontrer les bénéfices d'une architecture hexagonale.

### Différences avec l'étude de cas E-Commerce

| | E-Commerce | Banking |
|---|---|---|
| Structure | Mono-module | Multi-modules Maven |
| Java | 17 | 21 |
| Spring Boot | 3.2.5 | 3.5.10 |
| Domaine | Commandes, produits, paiements | Comptes, virements, cartes |
| Anti-pattern spécifique | Package plat | Découpage par couches techniques |

## Prérequis

- **Java 21** (installé localement, par exemple via Homebrew : `brew install openjdk@21`)

## Démarrage rapide

Si Java 21 n'est pas votre JDK par défaut, exportez `JAVA_HOME` avant chaque commande
(ou ajoutez l'export dans votre `.bashrc` / `.zshrc`) :

```bash
export JAVA_HOME="/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"
```

```bash
# Compiler et installer les modules dans le dépôt Maven local
./mvnw clean install

# Lancer l'application
./mvnw spring-boot:run -pl banking-app

# Accéder à la console H2
# http://localhost:8080/h2-console
# JDBC URL : jdbc:h2:mem:bankingdb
```

> **Note :** `clean install` est nécessaire avant `spring-boot:run` car le projet
> est multi-modules. Chaque module doit être installé dans le dépôt Maven local
> pour que `banking-app` puisse résoudre ses dépendances.

## Structure du projet

```
case-study-banking/
├── banking-core/          Modèle partagé, exceptions, utilitaires
├── banking-persistence/   Repositories JPA, configuration
├── banking-service/       Logique métier (@Service)
├── banking-api/           Controllers REST, DTOs
└── banking-app/           Assembly Spring Boot
```

### Dépendances entre modules

```
banking-app → banking-api → banking-service → banking-persistence → banking-core
                                             ↗
                             banking-service
```

## Branches Git

Chaque branche représente une étape de la migration progressive :

| Branche | Description |
|---------|-------------|
| `step/0-legacy` | Application legacy avec tous les anti-patterns |
| `step/1-discovery` | Premier audit HexaGlue, score initial |
| `step/2-configured` | Configuration des exclusions, amélioration du score |
| `step/3-hexagonal` | Restructuration des packages en architecture hexagonale |
| `step/4-pure-domain` | Purification du domaine (value objects, domain events, logique métier) |
| `step/5-generated` | Génération du code infrastructure par les plugins HexaGlue |
| `main` | Application fonctionnelle finale |

Voir [MIGRATION.md](MIGRATION.md) pour le détail de chaque étape.

## Domaine métier

L'application gère les opérations bancaires courantes :

- **Clients** : création, mise à jour, consultation
- **Comptes** : ouverture, dépôt, retrait, clôture (types : courant, épargne, professionnel)
- **Virements** : initiation, exécution, annulation avec vérification des soldes
- **Cartes bancaires** : émission, blocage, activation
- **Transactions** : historique, relevés par type
- **Bénéficiaires** : gestion des destinataires de virements

## Anti-patterns documentés

Cette application legacy illustre volontairement 11 anti-patterns courants,
notamment :

1. **Modèle anémique** : les entités sont de simples conteneurs de données sans logique métier
2. **Couplage JPA** : annotations `@Entity` et `@MappedSuperclass` sur les classes domaine
3. **Absence de ports** : les services dépendent directement des repositories Spring Data
4. **Primitives omniprésentes** : `String` pour IBAN, BIC, email, numéro de carte ; `BigDecimal` pour les montants
5. **Découpage horizontal** : modules Maven organisés par couche technique au lieu du domaine

La liste complète est disponible dans [MIGRATION.md](MIGRATION.md).

## API REST

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| `GET` | `/api/accounts` | Liste des comptes |
| `POST` | `/api/accounts` | Ouvrir un compte |
| `POST` | `/api/accounts/{id}/deposit` | Effectuer un dépôt |
| `POST` | `/api/accounts/{id}/withdraw` | Effectuer un retrait |
| `GET` | `/api/customers/{id}` | Consulter un client |
| `POST` | `/api/customers` | Créer un client |
| `POST` | `/api/transfers` | Initier un virement |
| `POST` | `/api/transfers/{id}/execute` | Exécuter un virement |
| `GET` | `/api/cards/account/{id}` | Cartes d'un compte |
| `GET` | `/api/transactions/account/{id}` | Historique des transactions |
