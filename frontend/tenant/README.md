# Tenant App

Angular portal for **tenants** to review leases, approve them, and access lease attachments.

## Implemented features

- **Lease review**
  - List + view leases for the current tenant
- **Dual approval flow**
  - Tenant approval is available only while `PENDING_APPROVAL`
  - The Lease service authorizes tenant actions using `X-Tenant-Id`
- **Attachments**
  - List + preview/download lease attachments (stored in MinIO, served via Lease service)
- **Identity → tenant mapping**
  - Primary: Keycloak claim `lms_tenant_id` (numeric Lease-service `tenants.id`)
  - Fallback: resolve by calling Lease service `GET /api/v1/tenants?externalPartyId=...` using
    `lms_external_party_id` (if present) else `preferred_username` else `sub`

## How to run

From repo root:

```bash
docker compose -f docker-compose.yml -f docker-compose.dev.yml up --build
```

- **URL (via nginx gateway)**: `http://localhost/tenant/`
- **Dev-only direct port (from `docker-compose.dev.yml`)**: `http://localhost:83/`

## Local development (Angular)

From this folder:

```bash
npm install
npm run start
```

For OIDC + same-origin API calls, access the app via the gateway at `http://localhost/tenant/`.

