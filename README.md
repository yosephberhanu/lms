# Lease Management System

Multi-service Lease Management System with Angular portals and Spring Boot microservices.

## What’s implemented

- **Property service**: owners, properties, and property ownerships.
- **Lease service**: lease lifecycle (draft → approval → active → terminated), tenant records, and **MinIO-backed attachments**.
- **Notification service**: in-app notifications + streaming updates.
- **Keycloak-admin service**: admin APIs for Keycloak users and realm roles.
- **Portals (Angular)**
  - **Manager**: manage properties/tenants/leases + attachments
  - **Owner**: review leases, approve as owner, view attachments
  - **Tenant**: review leases, approve as tenant, view attachments
  - **Admin**: manage users/roles + broadcast and read in-app messages

## Run (Docker Compose)

From this folder:

```bash
docker compose -f docker-compose.yml -f docker-compose.dev.yml up --build
```

### URLs

- **Gateway**: `http://localhost/`
- **Manager portal**: `http://localhost/manager/`
- **Owner portal**: `http://localhost/owner/`
- **Tenant portal**: `http://localhost/tenant/`
- **Admin portal**: `http://localhost/admin/`

### Dev convenience ports (from `docker-compose.dev.yml`)

- **Property service**: `http://localhost:8081/` (container 8080)
- **Lease service**: `http://localhost:8082/` (container 8080)
- **Keycloak-admin service**: `http://localhost:8083/` (container 8080)
- **Notification service**: `http://localhost:8084/` (container 8080)
- **Keycloak**: `http://localhost:8180/` (container 8080)

## API routing

The nginx gateway routes:

- `/api/property/**` → Property service
- `/api/lease/**` → Lease service
- `/api/notification/**` → Notification service (SSE supported)
- `/api/admin/**` → Keycloak-admin service

## Service docs

- `backend/property/README.md`
- `backend/lease/README.md`
- `frontend/manager/README.md`
- `frontend/owner/README.md`
- `frontend/tenant/README.md`
- `frontend/admin/README.md`
# Lease Management System

A microservices-based Lease Management System built with Spring Boot for managing properties, tenants, lease agreements, payments, billing, maintenance requests, documents, and notifications.

This project is intentionally designed as a **realistic but imperfect distributed system**. In addition to supporting core lease-management workflows, it is also meant to serve as a useful codebase for studying software architecture, service dependencies, mixed communication styles, observability gaps, and architectural anti-patterns.

## Goals

- Manage users, properties, leases, payments, invoices, maintenance requests, and documents
- Use a mix of synchronous and asynchronous communication
- Model realistic service boundaries and cross-service dependencies
- Provide infrastructure for API routing, config, discovery, messaging, storage, and monitoring
- Preserve some intentional architectural imperfections for research and visualization purposes

---

## Repository Structure

```text
.
├── billing
├── build.gradle
├── config
├── docker-compose.yml
├── docs
├── document
├── gateway
├── infra
│   ├── kafka
│   ├── keycloak
│   ├── minio
│   ├── mongo
│   ├── monitoring
│   ├── postgres
│   └── rabbitmq
├── lease
├── maintenance
├── notification
├── payment
├── property
├── README.md
├── registry
├── settings.gradle
├── shared
└── TODO.md
```


## Services

### Core Business Services

#### property
Manages properties/assets being leased.

__Responsibilities__:
* Store property details
* Manage ownership mappings
* Expose property information to other services

#### lease
Core domain service responsible for lease lifecycle management.

__Responsibilities__:
* Create and manage lease agreements
* Track lease states
* Connect tenants and properties
* Coordinate with Property, Payment, and other services

This service is intentionally more central than ideal to create richer dependency structures.

#### payment
Handles rent payment processing.

__Responsibilities__:
* Initiate payments
* Track payment status
* Integrate with a fake or mock external payment provider
* Publish payment-related events

#### billing
Generates invoices and tracks dues.

__Responsibilities__:
* Create monthly invoices
* Track due dates and late fees
* React to lease creation events
* Support scheduled billing logic

#### maintenance
Tracks maintenance and repair requests.

__Responsibilities__:
* Create maintenance tickets
* Track repair status
* Associate requests with properties and possibly leases

#### notification
Handles asynchronous notifications.

__Responsibilities__:
* Consume domain events
* Send mocked email/SMS notifications
* Track notification logs if needed

#### document
Stores lease-related documents and attachments.

__Responsibilities__:
* Upload and retrieve lease documents
* Store metadata
* Use MongoDB and/or object storage such as MinIO

⸻

### Platform / Infrastructure Services

#### gateway
API Gateway for external access.

__Responsibilities__:
* Route incoming requests
* Apply authentication and filtering
* Serve as the main client entry point

#### registry
Service discovery server.

__Responsibilities__:
* Register services
* Support service lookup for internal communication

#### config
Centralized configuration service.

__Responsibilities__:
* Provide externalized configuration to services
* Support environment-based settings

#### shared
Shared libraries and common code.

May contain:
* Common DTOs
* Event contracts
* Exception handling
* Logging helpers
* Utility classes

⸻

### Infrastructure

The infra/ directory contains local development infrastructure and support services.

#### infra/postgres

PostgreSQL setup for core relational services.

#### infra/mongo

MongoDB setup for document-related storage.

#### infra/minio

Object storage for uploaded files and lease documents.

#### infra/kafka

Kafka configuration for event-driven communication.

#### infra/rabbitmq

RabbitMQ configuration if RabbitMQ is used instead of or alongside Kafka.

#### infra/keycloak

Identity and access management setup. This may be used for future auth integration or experiments, even if security is currently lightweight or uneven across services.

#### infra/monitoring

Monitoring and observability stack, such as:
* Prometheus
* Grafana
* Zipkin
* Micrometer integrations

⸻

## Architecture Style

This system uses a mix of communication patterns.

### Synchronous communication

__Examples__:
* Lease → Property
* Lease → User/Auth/Identity-related lookup
* Payment → Lease

Asynchronous communication

__Examples__:
* Payment completed → Notification
* Lease created → Billing
* Maintenance created → Notification

This mixed style is intentional and reflects real-world distributed systems where not all interactions follow a single architectural pattern.

⸻

## Intentional Imperfections

This project is not meant to be a perfectly clean reference architecture.

It intentionally includes some architectural issues to make the system more useful for:
* architecture visualization
* program comprehension research
* dependency analysis
* observability studies
* onboarding experiments

Examples of intentional imperfections:
* Lease service is overly central
* Mixed REST and event usage
* Uneven observability across services
* Uneven security enforcement
* Possible shared database usage between some services
* Partial duplication of data across services
* Direct boundary violations in some places
* Inconsistent naming such as Property vs Asset

These are deliberate unless stated otherwise.

⸻

## Tech Stack

Planned / expected technologies include:
* Java
* Spring Boot
* Spring Web
* Spring Data JPA
* Spring Security
* PostgreSQL
* MongoDB
* MinIO
* Kafka and/or RabbitMQ
* Spring Cloud Gateway
* Eureka / service registry
* Spring Cloud Config
* Micrometer
* Prometheus
* Zipkin
* Gradle
* Docker Compose

⸻

## Build

From the project root:

```bash
./gradlew build
```

To build a specific module:

```bash
./gradlew :lease:build
```


⸻

Run with Docker Compose

Start infrastructure and services:

```bash
docker compose up --build
```
Run in background:
```bash
docker compose up -d --build
```
Stop everything:

```bash
docker compose down
```
Stop and remove volumes:

```bash
docker compose down -v
```

⸻

Suggested Startup Order

A practical startup order is:
1. PostgreSQL / MongoDB / MinIO
2. Kafka or RabbitMQ
3. Registry
4. Config
5. Gateway
6. Core business services:
	* property
	* lease
	* payment
	* billing
	* maintenance
	* notification
	* document

Depending on your docker-compose setup, this may already be automated with dependencies and health checks.

⸻

Current Development Status

This project is under active development.

Planned implementation areas include:
* service scaffolding
* database schemas
* domain models
* API contracts
* event contracts
* local infrastructure integration
* monitoring setup
* seed/demo data
* architecture documentation

See TODO.md￼ for the current implementation checklist.

⸻

Documentation

Additional documentation should go into the docs/ folder.

Suggested documents:
* architecture-overview.md
* service-catalog.md
* api-contracts.md
* event-flows.md
* deployment-notes.md
* intentional-imperfections.md

⸻

Notes for Contributors

When implementing or refactoring:
* preserve service boundaries unless there is a deliberate reason not to
* document architectural shortcuts clearly
* avoid hiding cross-service relationships behind too much abstraction
* keep the system runnable locally
* treat some imperfections as part of the design, not automatically as bugs

⸻

License

TBD