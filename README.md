# Primera Pulpa

> A full-stack inventory & production management web app for a nut-mix manufacturing plant, built with Spring Boot, Thymeleaf and PostgreSQL.

[![Java](https://img.shields.io/badge/Java-21-E76F00?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Thymeleaf](https://img.shields.io/badge/Thymeleaf-server--side-85B70E)](https://www.thymeleaf.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Maven](https://img.shields.io/badge/Maven-C71A36?logo=apachemaven&logoColor=white)](https://maven.apache.org/)

---

## Table of contents

- [Overview](#overview)
- [Key features](#key-features)
- [Tech stack](#tech-stack)
- [Project structure](#project-structure)
- [Getting started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Configuration](#configuration)
  - [Running the app](#running-the-app)
  - [Running the tests](#running-the-tests)
- [Documentation](#documentation)

---

## Overview

Primera Pulpa covers the day-to-day operation of a production plant, from raw material receiving to final sale:

- **Raw materials** tracked by batch and expiration date for full traceability.
- **Recipes (formulas)** with automatic cost-per-kilo calculation.
- **Production runs** that deduct stock automatically using **FEFO** (First Expired, First Out).
- **Orders workflow** with Pending / Delivered / Cancelled states and stock deduction on delivery.
- **Sales statistics**: annual balance, month-by-month evolution and profitability per product.
- **Role-based access control** with two profiles: *Administrator* and *Employee*.

Designed as a server-side rendered monolith: no SPA build step, plain Thymeleaf templates, Spring Security guarding every route.

## Key features

| Module | Description |
| --- | --- |
| Dashboard | Low-stock alerts, upcoming expirations and pending orders |
| Raw materials | CRUD with price updates and safe deletion (blocked when used in a formula) |
| Units of measure | Configurable purchase/weighing units |
| Goods receiving | Batch + expiration intake; records are immutable once confirmed |
| Formulas | Per-mix recipes with computed production cost |
| Additional costs | Packaging, labels and labor folded into the final cost |
| Mixes | Finished-product catalogue with sale prices |
| Production | Daily elaboration with automatic or manual batch consumption |
| Customers & orders | Sales, dispatch and stock deduction on delivery |
| Stock movements | In/out history with running balance (audit trail) |
| Statistics | Annual balance, monthly trends and per-mix breakdown |
| Security | Users, roles, backups and restore |

## Tech stack

- **Java 21**
- **Spring Boot 4.1** — Web MVC, Data JPA, Security, Validation
- **Thymeleaf** with `thymeleaf-extras-springsecurity6`
- **PostgreSQL**
- **MapStruct 1.6.3** & **Lombok**
- **Maven** (Maven Wrapper included)
- **JUnit / Spring Boot Test**

## Project structure

```
Primera-Pulpa/
├── README.md
├── Manual de usuario.md        # Step-by-step user guide (Spanish)
├── Documetation/               # Project manual, user stories, ER diagram, charter
└── Server/
    ├── pom.xml
    └── src/
        ├── main/
        │   ├── java/com/primeraPulpa/
        │   └── resources/
        │       ├── application.properties
        │       └── templates/   # Thymeleaf views
        └── test/java/com/primeraPulpa/
```

## Getting started

### Prerequisites

- JDK 21+
- Maven 3.8+ (or use the bundled wrapper `./mvnw`)
- A running PostgreSQL instance

### Configuration

The database connection is read from environment variables, declared in `Server/src/main/resources/application.properties`:

```properties
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USER_NAME}
spring.datasource.password=${DB_PASSWORD}
```

Set them before starting the server:

```bash
export DB_URL="jdbc:postgresql://localhost:5432/primera_pulpa"
export DB_USER_NAME="postgres"
export DB_PASSWORD="your_password"
```

The server listens on port **8080**.

### Running the app

```bash
cd Server

# Linux / macOS
./mvnw spring-boot:run

# Windows
mvnw.cmd spring-boot:run
```

Or build and run the jar:

```bash
./mvnw clean package
java -jar target/demo-0.0.1-SNAPSHOT.jar
```

Then open [http://localhost:8080](http://localhost:8080).

### Running the tests

```bash
cd Server
./mvnw test
```

The suite covers services, the production flow, security rules and stock movements.

## Documentation

- **[User manual](./Manual%20de%20usuario.md)** — full walkthrough of every screen (Spanish).
- `Documetation/` — project manual, user stories, ER diagram and project charter.
