# Admin App

Angular portal for **system administrators**. This UI is focused on **Keycloak user management** and **in-app notifications**.

## Implemented features

- **Users**
  - List/search Keycloak users
  - Create/edit users (including realm role assignment)
  - Delete users
- **Messages (in-app inbox)**
  - View your in-app inbox messages
  - Mark messages as read
  - Live refresh via notification stream updates
- **Broadcast**
  - Broadcast in-app messages to:
    - selected users and/or
    - all users in selected Keycloak realm roles
  - Recipients are merged and de-duplicated

## How to run

From repo root:

```bash
docker compose -f docker-compose.yml -f docker-compose.dev.yml up --build
```

- **URL (via nginx gateway)**: `http://localhost/admin/`
- **Dev-only direct port (from `docker-compose.dev.yml`)**: `http://localhost:84/`

## Local development (Angular)

From this folder:

```bash
npm install
npm run start
```

For OIDC + same-origin API calls, access the app via the gateway at `http://localhost/admin/`.
