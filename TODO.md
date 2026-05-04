# TODO

Backlog for the Lease Management System. Items are not strictly ordered; pick by dependency and product priority.

- [ ] **Maintenance** — **Maintenance** service + API: tickets tied to property/lease; events to **notification**; optional owner/manager UI.
- [ ] **Lease application** — Intake workflow (apply → review → create draft lease); optional manager approval before `submit-for-approval`.

- [ ] **Payments** — Implement `GET /leases/{id}/payments` and wire **payment** service; idempotency, webhooks, reconciliation with lease rent schedule.
- [ ] **Billing** — Invoice generation from lease events; wire **billing** service and DB; late fees and dunning (even if mocked at first).

- [ ] **Observability** — Enable `infra/monitoring` in compose; Micrometer metrics, structured logs, correlation IDs across gateway → services; optional tracing (Zipkin).

---

## Security and identity

- [ ] **Gateway authorization** — Enforce authn/authz on routes (`/api/**`, `/manager/`, `/owner/`); propagate identity headers or JWT to downstream services consistently.
- [ ] **Service-to-service auth** — Replace or harden trust assumptions for internal REST calls (e.g. lease → property); mTLS or signed internal tokens if required.

---

## DevOps and repo hygiene

- [ ] **Gradle / modules** — Bring optional modules (`payment`, `billing`, `maintenance`, `document`, `notification`) into `settings.gradle` when implemented, or document as separate repos.
- [ ] **CI** — Build, test, Docker image publish; compose smoke job against seeded Postgres.
- [ ] **Config consistency** — Audit Spring Cloud Config vs local `application.yml` (ports, `LEASE_PROPERTY_SERVICE_BASE_URL`, Eureka vs direct URIs) for all services in Docker.

---

## Nice-to-have / research

- [ ] **Event bus** — Kafka/RabbitMQ paths from README: lease-created → billing, payment-completed → notification, etc.
- [ ] **API versioning / deprecation policy** — Document breaking-change process for `/api/v1`.
- [ ] **Multi-region / tenancy** — Only if product scope expands.
