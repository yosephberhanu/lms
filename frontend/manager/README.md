# Manager App

Angular portal for **managers** to administer **properties**, **tenants**, and the **lease lifecycle** (including MinIO-backed attachments).

## Implemented features

- **Properties**
  - Browse/search properties
  - Lease-aware availability: properties surface whether there is a blocking lease state (`DRAFT`, `PENDING_APPROVAL`, `ACTIVE`)
- **Tenants (in Lease service)**
  - Create/search/manage tenant records used by leases
- **Leases**
  - Create/edit leases (starts as `DRAFT`)
  - Submit for approval (`DRAFT` → `PENDING_APPROVAL`)
  - Terminate active leases (`ACTIVE` → `TERMINATED`)
  - Owner is inferred from Property ownership (prefers `PRIMARY_OWNER`)
- **Attachments**
  - Upload/list/download/delete attachments on a lease while `DRAFT` or `PENDING_APPROVAL`
  - Attachment objects are stored in **MinIO**, with relational metadata in the Lease service

## How to run

- **Full stack (recommended)**: from repo root

```bash
docker compose -f docker-compose.yml -f docker-compose.dev.yml up --build
```

- **URL (via nginx gateway)**: `http://localhost/manager/`
- **Dev-only direct port (from `docker-compose.dev.yml`)**: `http://localhost:81/`

## Local development (Angular)

From this folder:

```bash
npm install
npm run start
```

This app uses `proxy.conf.json`. For most end-to-end flows (OIDC + same-origin APIs), access it via the gateway at `http://localhost/manager/`.
