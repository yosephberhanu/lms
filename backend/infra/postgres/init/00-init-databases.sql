-- Ensure databases exist before any per-db seed scripts run.
-- The docker-entrypoint-initdb.d scripts run in filename sort order.

CREATE DATABASE property_db;
CREATE DATABASE lease_db;
CREATE DATABASE payment_db;
CREATE DATABASE billing_db;
CREATE DATABASE maintenance_db;
CREATE DATABASE notification_db;

