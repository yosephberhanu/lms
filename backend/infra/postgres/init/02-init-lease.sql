-- Dev seed for Lease Service (lease_db).
-- Runs from docker-entrypoint-initdb.d after 00-init-databases.sql and 01-init-properties.sql.
-- Executes only when the postgres data volume is first initialized.
--
-- Consistency with property_db (01-init-properties.sql), assuming both seeds run on a fresh volume:
--   property_id 1 = Maple Grove (owners user-1001, user-1002) — lease owner_id uses user-1001 only here.
--   property_id 2 = Downtown Office (owner org-2005) — lease owner_id org-2005.
--   property_id 3 = Blue Ridge Storage (owner user-1003).
--   property_id 4 = Riverbend Retail — no leases in this seed (reserved).
--   property_id 5 = Cedar Hill Duplexes (user-1005, user-1006) — leases use user-1005 or user-1006 as owner_id.
-- Every lease.owner_id is a party_id that appears on property_ownerships for that property_id.
-- tenant_id 1..8 match insert order in this file (BIGSERIAL from 1).

\connect lease_db;

CREATE TABLE IF NOT EXISTS tenants (
    id BIGSERIAL PRIMARY KEY,
    external_party_id VARCHAR(128),
    display_name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(50),
    status VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS leases (
    id BIGSERIAL PRIMARY KEY,
    property_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id),
    owner_id VARCHAR(128),
    monthly_rent NUMERIC(14, 2) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(32) NOT NULL,
    deposit_amount NUMERIC(14, 2),
    payment_schedule VARCHAR(255),
    property_name_snapshot VARCHAR(512),
    tenant_name_snapshot VARCHAR(512),
    owner_approved_at TIMESTAMPTZ,
    tenant_approved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS lease_status_history (
    id BIGSERIAL PRIMARY KEY,
    lease_id BIGINT NOT NULL REFERENCES leases(id) ON DELETE CASCADE,
    old_status VARCHAR(32),
    new_status VARCHAR(32) NOT NULL,
    changed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    changed_by VARCHAR(255)
);

-- Insert only if empty (property_id 1–2 match 01-init-properties.sql when property seed ran)
DO $$
BEGIN
    IF (SELECT COUNT(*) FROM tenants) = 0 THEN
        INSERT INTO tenants (external_party_id, display_name, email, phone, status)
        VALUES
            -- Existing
            ('tenant-user-5001', 'Sarah Johnson',     'sarah.johnson@example.com',     '+1-540-555-0311', 'ACTIVE'),
            ('tenant-user-5002', 'Marcus Lee',        'marcus.lee@example.com',        '+1-540-555-0322', 'ACTIVE'),
            ('tenant-user-5003', 'Olivia Bennett',    'olivia.bennett@example.com',    NULL,              'ACTIVE'),
            ('tenant-biz-6001',  'NextGen Analytics LLC', 'contact@nextgenanalytics.com', '+1-540-555-0333', 'ACTIVE'),

            -- New individuals
            ('tenant-user-5004', 'Ethan Walker',      'ethan.walker@example.com',      '+1-540-555-0344', 'ACTIVE'),
            ('tenant-user-5005', 'Maya Singh',        'maya.singh@example.com',        '+1-540-555-0355', 'ACTIVE'),
            ('tenant-user-5006', 'Noah Kim',          'noah.kim@example.com',          NULL,              'ACTIVE'),

            -- New business tenant
            ('tenant-biz-6002',  'BluePeak Marketing', 'hello@bluepeakmarketing.com',  '+1-540-555-0366', 'ACTIVE');

        INSERT INTO leases (
            property_id, tenant_id, owner_id, monthly_rent, start_date, end_date,
            status, deposit_amount, payment_schedule,
            property_name_snapshot, tenant_name_snapshot
        )
        VALUES
            -- Existing leases
            (1, 1, 'user-1001', 1200.00, DATE '2025-01-01', DATE '2025-12-31', 'ACTIVE', 1200.00, 'MONTHLY', 'Maple Grove Apartments', 'Sarah Johnson'),
            (1, 2, 'user-1001', 1150.00, DATE '2025-02-01', DATE '2026-01-31', 'ACTIVE', 1150.00, 'MONTHLY', 'Maple Grove Apartments', 'Marcus Lee'),
            (2, 4, 'org-2005', 3500.00, DATE '2024-06-01', DATE '2027-05-31', 'ACTIVE', 7000.00, 'MONTHLY', 'Downtown Office Suites', 'NextGen Analytics LLC'),
            (5, 3, 'user-1005', 1400.00, DATE '2026-01-01', DATE '2026-12-31', 'DRAFT', 1400.00, 'MONTHLY', 'Cedar Hill Duplexes', 'Olivia Bennett'),

            -- NEW: more residential tenants (same apartment complex)
            (1, 5, 'user-1001', 1250.00, DATE '2024-09-01', DATE '2025-08-31', 'ACTIVE', 1250.00, 'MONTHLY', 'Maple Grove Apartments', 'Ethan Walker'),
            (1, 6, 'user-1001', 1100.00, DATE '2023-08-01', DATE '2024-07-31', 'EXPIRED', 1100.00, 'MONTHLY', 'Maple Grove Apartments', 'Maya Singh'),

            -- NEW: Cedar Hill Duplexes (second unit)
            (5, 7, 'user-1006', 1350.00, DATE '2025-03-01', DATE '2026-02-28', 'ACTIVE', 1350.00, 'MONTHLY', 'Cedar Hill Duplexes', 'Noah Kim'),

            -- NEW: additional commercial tenant
            (2, 8, 'org-2005', 2800.00, DATE '2025-04-01', DATE '2028-03-31', 'ACTIVE', 5600.00, 'MONTHLY', 'Downtown Office Suites', 'BluePeak Marketing'),

            -- NEW: industrial property usage
            (3, 5, 'user-1003', 900.00, DATE '2024-01-01', DATE '2024-12-31', 'EXPIRED', 900.00, 'MONTHLY', 'Blue Ridge Storage', 'Ethan Walker');

        INSERT INTO lease_status_history (lease_id, old_status, new_status, changed_by)
        VALUES
            -- Existing histories
            (1, NULL, 'ACTIVE', 'seed'),
            (2, NULL, 'DRAFT', 'seed'),
            (2, 'DRAFT', 'ACTIVE', 'seed'),
            (3, NULL, 'DRAFT', 'seed'),
            (3, 'DRAFT', 'PENDING_APPROVAL', 'seed'),
            (3, 'PENDING_APPROVAL', 'ACTIVE', 'seed'),
            (4, NULL, 'DRAFT', 'seed'),

            -- NEW histories
            (5, NULL, 'ACTIVE', 'seed'),
            (6, NULL, 'ACTIVE', 'seed'),
            (6, 'ACTIVE', 'EXPIRED', 'system'),

            (7, NULL, 'DRAFT', 'seed'),
            (7, 'DRAFT', 'ACTIVE', 'seed'),

            (8, NULL, 'DRAFT', 'seed'),
            (8, 'DRAFT', 'ACTIVE', 'seed'),

            (9, NULL, 'ACTIVE', 'seed'),
            (9, 'ACTIVE', 'EXPIRED', 'system');

        -- Lease 3 history includes PENDING_APPROVAL → ACTIVE; align row with dual-approval timestamps.
        UPDATE leases
        SET owner_approved_at = TIMESTAMPTZ '2024-06-10 09:00:00+00',
            tenant_approved_at = TIMESTAMPTZ '2024-06-11 11:00:00+00'
        WHERE id = 3
          AND status = 'ACTIVE';
    END IF;
END $$;
-- Keep SERIAL sequences past the highest id (next insert gets MAX(id)+1; empty table → next id1)
SELECT setval(
    pg_get_serial_sequence('tenants', 'id'),
    COALESCE((SELECT MAX(id) FROM tenants), 0),
    (SELECT MAX(id) FROM tenants) IS NOT NULL
);
SELECT setval(
    pg_get_serial_sequence('leases', 'id'),
    COALESCE((SELECT MAX(id) FROM leases), 0),
    (SELECT MAX(id) FROM leases) IS NOT NULL
);
SELECT setval(
    pg_get_serial_sequence('lease_status_history', 'id'),
    COALESCE((SELECT MAX(id) FROM lease_status_history), 0),
    (SELECT MAX(id) FROM lease_status_history) IS NOT NULL
);
