# Owner App

Angular portal for **property owners** to review leases, approve them, and access lease attachments.

## Implemented features

- **Lease visibility**
  - Owners can view leases for properties they own (via Property service ownership mappings)
  - Approval is restricted to the designated `lease.owner_id`
- **Dual approval flow**
  - Owner approval is available only while `PENDING_APPROVAL`
  - The Lease service authorizes owner actions using `X-Owner-Party-Id`
- **Attachments**
  - List + preview/download lease attachments (stored in MinIO, served via Lease service)
- **Identity → party id mapping**
  - Primary: Keycloak claim `lms_party_id`
  - Fallback: `preferred_username`, then `sub`
  - Must match `property_ownerships.owner_party_id` and `leases.owner_id` (e.g. `user-1001`, `org-2005`)

## How to run

From repo root:

```bash
docker compose -f docker-compose.yml -f docker-compose.dev.yml up --build
```

- **URL (via nginx gateway)**: `http://localhost/owner/`
- **Dev-only direct port (from `docker-compose.dev.yml`)**: `http://localhost:82/`

## Local development (Angular)

From this folder:

```bash
npm install
npm run start
```

For OIDC + same-origin API calls, access the app via the gateway at `http://localhost/owner/`.
