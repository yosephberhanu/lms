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
