# Lease Service

Core lease lifecycle API: create → submit for approval → **owner and tenant must each approve** → then active → terminate.

Also provides **MinIO-backed lease attachments** (upload/list/download/delete) stored as objects with relational metadata.

Integrates with **Property** over HTTP and optionally with **User** when `lease.user-service.base-url` is set.

## Implemented domain

- **Tenants**: tenant records live in this service and are referenced by leases
- **Leases**: draft → pending approval → active → terminated
- **Dual approvals**: owner approval (`X-Owner-Party-Id`) + tenant approval (`X-Tenant-Id`)
- **Attachments**: S3-compatible object storage via MinIO, with relational metadata

## Run locally

```bash
# Postgres + Eureka (and Property, required for validation calls)
docker compose up -d postgres registry property

./gradlew :lease:bootRun
```

Default URL: **http://localhost:8082**  
Swagger UI: **http://localhost:8082/swagger-ui.html**

## Configuration

| Variable | Purpose |
|----------|---------|
| `SPRING_DATASOURCE_URL` | Default `jdbc:postgresql://localhost:5432/lease_db` |
| `LEASE_PROPERTY_SERVICE_BASE_URL` | Property service root (must match **property** `server.port`, usually **8080** — e.g. `http://localhost:8080` or `http://property:8080` in Compose) |
| `LEASE_USER_SERVICE_BASE_URL` | If empty, tenant existence is **not** checked. If set, `GET {base}/users/{id}` is called. |
| `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` | Eureka (optional for local-without-registry experiments) |
| `LEASE_ATTACHMENTS_S3_ENDPOINT` | S3-compatible endpoint (MinIO). In Docker Compose, use `http://minio:9000` (not `localhost`). |
| `LEASE_ATTACHMENTS_S3_ACCESS_KEY` | S3 access key (MinIO root user in local dev). |
| `LEASE_ATTACHMENTS_S3_SECRET_KEY` | S3 secret key (MinIO root password in local dev). |
| `LEASE_ATTACHMENTS_S3_BUCKET` | Bucket name for attachments (default `lms-lease-attachments`). |

## Tenants (recorded in this service)

Create tenants before creating leases that reference them.

| Method | Path | Notes |
|--------|------|--------|
| GET | `/tenants` | Query: `q`, `externalPartyId`, `displayName`, `email`, `phone` |
| GET | `/tenants/{id}` | |
| POST | `/tenants` | Body: `displayName`, optional `externalPartyId` (User Service id), `email`, `phone`, `status` |
| PUT | `/tenants/{id}` | |
| DELETE | `/tenants/{id}` | Fails if any lease references this tenant |

`LeaseWriteRequest.tenantId` is the **numeric id** returned from `POST /tenants`.

## API (prefix `/api/v1`)

| Method | Path | Notes |
|--------|------|--------|
| GET | `/leases` | Query: `propertyId`, `tenantId` (tenant row id), `status`, `ownerId` (exact match on lease `owner_id`), `propertyOwnerPartyId` (leases whose `property_id` is in properties returned by Property service `GET /properties?ownerPartyId=` — any co-owner sees all leases on those buildings) |
| GET | `/leases/{id}` | |
| POST | `/leases` | Creates **DRAFT**; publishes in-process `LeaseCreatedEvent` |
| PUT | `/leases/{id}` | Only **DRAFT** or **PENDING_APPROVAL** |
| POST | `/leases/{id}/submit-for-approval` | **DRAFT** → **PENDING_APPROVAL** (clears any prior approval timestamps) |
| POST | `/leases/{id}/approve-as-owner` | While **PENDING_APPROVAL**, records owner approval; if tenant already approved → **ACTIVE**. **Required header:** `X-Owner-Party-Id` must equal the lease’s `owner_id`. |
| POST | `/leases/{id}/approve-as-tenant` | While **PENDING_APPROVAL**, records tenant approval; if owner already approved → **ACTIVE**. **Required header:** `X-Tenant-Id` must equal the lease’s tenant row id. |
| POST | `/leases/{id}/terminate` | **ACTIVE** → **TERMINATED** |
| GET | `/leases/{id}/payments` | Empty placeholder (Payment service) |
| GET | `/leases/{id}/documents` | Empty placeholder (Document service) |

### Attachments API (prefix `/api/v1`)

Attachments are stored in MinIO and linked to a lease via `lease_attachments` metadata.

| Method | Path | Notes |
|--------|------|--------|
| GET | `/leases/{leaseId}/attachments` | Manager: no headers. Owner: `X-Owner-Party-Id` must match `lease.owner_id`. Tenant: `X-Tenant-Id` must match lease `tenant.id`. |
| POST | `/leases/{leaseId}/attachments` | Upload multipart field `file`. **Manager only**. Allowed only when lease is `DRAFT` or `PENDING_APPROVAL`. |
| GET | `/leases/{leaseId}/attachments/{attachmentId}` | Download. Same authz rules as list. |
| DELETE | `/leases/{leaseId}/attachments/{attachmentId}` | **Manager only**. Allowed only when lease is `DRAFT` or `PENDING_APPROVAL`. |

## Gateway

When running the full stack, the nginx gateway routes:

- `http://localhost/api/lease/...` → Lease service

Some environments also route via Eureka (Spring Cloud style) using a prefix like **`/lease/**`** with the prefix stripped.
