# Property Service

Spring Boot service for **Properties**, **Property Ownership**, and **Owners** in the Lease Management System.

## Implemented domain

- **Owners** (`partyId`, name/contact fields)
- **Properties** (type, address, descriptive fields)
- **Property ownerships** (links properties to owner `partyId`, with ownership type such as primary/co-owner)

## Running locally

### Prerequisites
- Java **21**
- Postgres (or run via Docker Compose)

### Start dependencies (recommended)
From repo root:

```bash
docker compose up -d postgres registry config
```

### Run the service (Gradle)
From repo root:

```bash
./gradlew :property:bootRun
```

The service listens on **`8080`** by default (`server.port` in `application.yml`). With `docker-compose.dev.yml`, host port **8081** is mapped to container **8080**.

### Run via Docker Compose (full stack)
From repo root:

```bash
docker compose up --build property
```

## Configuration

Default config lives in `property/src/main/resources/application.yml`.

Common environment variables:
- **`SPRING_DATASOURCE_URL`**: defaults to `jdbc:postgresql://localhost:5432/property_db`
- **`SPRING_DATASOURCE_USERNAME`**: defaults to `postgres`
- **`SPRING_DATASOURCE_PASSWORD`**: defaults to `postgres`
- **`EUREKA_CLIENT_SERVICEURL_DEFAULTZONE`**: defaults to `http://localhost:8761/eureka/`

## API Docs (Swagger UI)

When running the service:
- Swagger UI: `http://localhost:8080/swagger-ui.html` (or `http://localhost:8081/...` when using dev compose port mapping)

## Gateway routing (nginx)

When running the full stack, the gateway routes:

- `GET http://localhost/api/property/...` → Property service

## REST API

Base path: **`/api/v1`**

### Owners

- **List / search / filter**
  - `GET /api/v1/owners`
  - Query params (all optional):
    - `q`: free-text search across `partyId`, `displayName`, `email`, `phone`
    - `partyId`: exact match
    - `displayName`: contains (case-insensitive)
    - `email`: contains (case-insensitive)
    - `phone`: contains (case-insensitive)

Examples:

```bash
curl "http://localhost:8080/api/v1/owners?q=org-2001"
curl "http://localhost:8080/api/v1/owners?displayName=owner"
```

- **Get by id**: `GET /api/v1/owners/{id}`
- **Create**: `POST /api/v1/owners`
- **Replace**: `PUT /api/v1/owners/{id}`
- **Delete**: `DELETE /api/v1/owners/{id}`

### Properties

- **List / search / filter**
  - `GET /api/v1/properties`
  - Query params (all optional):
    - `q`: free-text search across name/address/city/state/postal/country/description
    - `city`: contains (case-insensitive)
    - `country`: contains (case-insensitive)
    - `propertyType`: enum (e.g. `RESIDENTIAL`, `COMMERCIAL`, `LAND`, ...)
    - `ownerPartyId`: when set, only properties that have an ownership row for this owner party id are returned

Examples:

```bash
curl "http://localhost:8080/api/v1/properties?propertyType=RESIDENTIAL&city=Addis"
curl "http://localhost:8080/api/v1/properties?q=airport"
curl "http://localhost:8080/api/v1/properties?ownerPartyId=user-1001"
```

- **Get by id**: `GET /api/v1/properties/{id}`
- **Create**: `POST /api/v1/properties`
- **Replace**: `PUT /api/v1/properties/{id}`
- **Delete**: `DELETE /api/v1/properties/{id}`

### Property ownerships (nested under a property)

- `GET /api/v1/properties/{propertyId}/ownerships`
- `GET /api/v1/properties/{propertyId}/ownerships/{ownershipId}`
- `POST /api/v1/properties/{propertyId}/ownerships`
- `PUT /api/v1/properties/{propertyId}/ownerships/{ownershipId}`
- `DELETE /api/v1/properties/{propertyId}/ownerships/{ownershipId}`

## Database notes

### Dev seed SQL (Docker only)
The repo includes a dev seed script at `infra/postgres/init/01-init-properties.sql` that creates and seeds tables for the property service **when the Postgres volume is initialized from scratch**.

### JPA schema management
The service currently runs with:
- `spring.jpa.hibernate.ddl-auto=update`

This is convenient for development, but you may want to switch to migrations for production.

