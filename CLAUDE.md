# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What This Is

Mock AISP (EAD simulator) for testing DNOTAM Consumer implementations. Simulates EUROCONTROL's Digital NOTAM Subscription and Request Service: REST subscription management (SPEC-170), AMQP event publishing via ActiveMQ Artemis, heartbeats, and 82 sample AIXM 5.1.1 events across 13 scenario types.

This is what `swim-dnotam-consumer` connects to during development. **Never the other way around.**

## Build and Run

```bash
# Install shared dependencies first (one-time setup)
# Clone and install swim-developer-validators + siblings:
make sync

# Start infrastructure (Artemis on 5674, MariaDB on 3307)
podman compose up -d

# Run in dev mode (port 8084, mTLS disabled, Swagger at /swagger-ui)
./mvnw quarkus:dev

# Build (compiles tests but skips execution)
./mvnw clean package -DskipTests

# Run tests (uses Testcontainers — requires Podman running)
./mvnw verify -DskipITs=false

# Run a single test class
./mvnw test -Dtest=AixmEventValidationTest

# Coverage
./mvnw test jacoco:report
# Report at target/site/jacoco/index.html
```

**Maven builds require network access** — the Quarkus plugin resolves artifacts at build time via Maven Aether. Sandboxed environments will fail.

## Architecture

Hexagonal architecture. This project extends shared modules from `swim-developer-validators` (parent POM: `swim-validators`).

### Package Structure (`com.github.swim_developer.validator.dnotam.consumer`)

```
domain/
  model/          — Domain entities (EventFileMetadata, FilterOptions)
  port/in/        — Inbound ports (DnotamTopicPort, DnotamEventMetadataPort)
application/
  usecase/        — Port implementations (DnotamTopicService, DnotamEventMetadataService)
infrastructure/
  rest/           — JAX-RS resources (subscription, topics, WFS, admin, UI)
  rest/dto/       — Request/response DTOs
  config/         — OpenAPI and reflection configuration
  util/           — XML utilities (XmlIdRandomizer)
```

### Key Dependencies from Parent

Most business logic lives in the shared `swim-validator-consumer` module (subscription lifecycle, AMQP publishing, event generation, heartbeats). This project adds DNOTAM-specific concerns: topic catalog, event metadata, OpenAPI descriptions, and AIXM event XML files.

Core types from shared modules:
- `ManageSubscriptionPort` — subscription CRUD operations (framework)
- `SubscriptionResponse`, `TopicSummary`, `TopicDetails` — domain models (framework)
- `CreateSubscriptionCommand` — command object for subscription creation

### Infrastructure

| Service | Port | Purpose |
|---------|------|---------|
| Artemis | 5674 (AMQP), 8163 (console) | Event delivery broker |
| MariaDB | 3307 | Subscription persistence |
| Validator app (dev) | 8084 | REST API |

### Profiles

- `%dev` — local development, mTLS disabled, connects to compose services
- `%test` — Testcontainers (DevServices), AMQP/events/heartbeat disabled
- `%prod` — full mTLS, external broker/DB config via env vars

## Code Standards

- **JSON processing**: `jq` only in shell, never python/node
- **K8s resources**: YAML files only, never inline `oc create`

## Domain Context

SWIM = System Wide Information Management (ICAO mandate for aviation data exchange). DNOTAM = Digital NOTAM, the machine-readable replacement for text-based NOTAMs. Data format is AIXM 5.1.1 (XML). Protocol stack: REST for subscriptions, AMQP 1.0 for event distribution, WFS 2.0 for queries. Security: mTLS with EACP (European Aviation Common PKI) certificates.

CP1 event scenarios: RWY.CLS, AD.CLS, TWY.CLS, APN.CLS, RWY.LIM, AD.LIM, SFC.CON, SAA.ACT, SAA.NEW, OBS.NEW, NAV.UNS, STAND.LIM, RCP.CHG.
