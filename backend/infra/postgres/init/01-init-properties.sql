-- Dev seed for Property Service.
-- This file is mounted only in docker-compose.dev.yml.
-- It runs only when the postgres volume is initialized (fresh volume).
--
-- Property ids (insert order) align with lease_db seed (02-init-lease.sql):
--   id 1 Maple Grove, id 2 Downtown Office, id 3 Blue Ridge Storage,
--   id 4 Riverbend Retail (no sample leases yet), id 5 Cedar Hill Duplexes.
-- owner_party_id values are a subset of the owners.party_id seed list below.

\connect property_db;

CREATE TABLE IF NOT EXISTS owners (
    id BIGSERIAL PRIMARY KEY,
    party_id VARCHAR(128) NOT NULL UNIQUE,
    display_name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(50),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS properties (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    address_line1 VARCHAR(255) NOT NULL,
    address_line2 VARCHAR(255),
    city VARCHAR(255) NOT NULL,
    state_or_province VARCHAR(255) NOT NULL,
    postal_code VARCHAR(255) NOT NULL,
    country VARCHAR(255) NOT NULL,
    property_type VARCHAR(64) NOT NULL,
    description VARCHAR(2000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS property_ownerships (
    id BIGSERIAL PRIMARY KEY,
    property_id BIGINT NOT NULL REFERENCES properties(id) ON DELETE CASCADE,
    owner_party_id VARCHAR(128) NOT NULL,
    role VARCHAR(64) NOT NULL,
    ownership_percentage NUMERIC(5,2) NOT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE,
    notes VARCHAR(1000)
);

-- Insert only if empty
DO $$
BEGIN
    IF (SELECT COUNT(*) FROM owners) = 0 THEN
        INSERT INTO owners (party_id, display_name, email, phone)
        VALUES
        -- Individuals
        ('user-1001', 'James Carter',        'james.carter@example.com',        '+1-540-555-0134'),
        ('user-1002', 'Emily Nguyen',        'emily.nguyen@example.com',        '+1-540-555-0198'),
        ('user-1003', 'Michael Thompson',    'm.thompson@example.com',          '+1-540-555-0172'),
        ('user-1004', 'Sofia Ramirez',       'sofia.ramirez@example.com',       '+1-540-555-0113'),
        ('user-1005', 'Daniel Brooks',       'daniel.brooks@example.com',       '+1-540-555-0149'),
        ('user-1006', 'Ava Patel',           'ava.patel@example.com',           '+1-540-555-0186'),
        ('user-1007', 'Liam O’Connor',       'liam.oconnor@example.com',        '+1-540-555-0165'),
        ('user-1008', 'Olivia Chen',         'olivia.chen@example.com',         '+1-540-555-0127'),

        -- Small businesses / organizations
        ('org-2001',  'Blue Ridge Farms',    'contact@blueridgefarms.com',      '+1-540-555-0221'),
        ('org-2002',  'New River Realty',    'info@newriverrealty.com',         '+1-540-555-0254'),
        ('org-2003',  'Appalachian Woodworks','sales@appwoodworks.com',         '+1-540-555-0217'),
        ('org-2004',  'Mountain View HVAC',  'service@mvhvac.com',              '+1-540-555-0289'),
        ('org-2005',  'Horizon IT Solutions','support@horizonit.com',           '+1-540-555-0233'),
        ('org-2006',  'Green Valley Landscaping','hello@greenvalleyland.com',  '+1-540-555-0266'),
        ('org-2007',  'Summit Fitness Center','contact@summitfitness.com',      '+1-540-555-0242'),

        -- Mixed / edge-style entries
        ('user-1009', 'Chris Johnson Jr.',   'chris.johnson@example.com',       NULL),
        ('user-1010', 'Taylor Morgan',       NULL,                              '+1-540-555-0155'),
        ('org-2008',  'Riverbend Consulting','info@riverbendconsulting.com',    NULL);
    END IF;

    IF (SELECT COUNT(*) FROM properties) = 0 THEN
    INSERT INTO properties
        (name, address_line1, address_line2, city, state_or_province, postal_code, country, property_type, description)
    VALUES
        ('Maple Grove Apartments', '1200 Maple Grove Ln', NULL, 'Blacksburg', 'VA', '24060', 'US', 'RESIDENTIAL',
         'Mid-size apartment community with 24 units catering to students and young professionals.'),

        ('Downtown Office Suites', '101 College Ave', 'Suite 200', 'Blacksburg', 'VA', '24060', 'US', 'COMMERCIAL',
         'Modern office suites located in the downtown business district.'),

        ('Blue Ridge Storage', '4500 Harding Rd', NULL, 'Christiansburg', 'VA', '24073', 'US', 'INDUSTRIAL',
         'Self-storage and light industrial facility serving the New River Valley.'),

        ('Riverbend Retail Center', '780 University City Blvd', NULL, 'Blacksburg', 'VA', '24060', 'US', 'COMMERCIAL',
         'Neighborhood retail center with multiple tenants and parking.'),

        ('Cedar Hill Duplexes', '55 Cedar Hill Dr', NULL, 'Radford', 'VA', '24141', 'US', 'RESIDENTIAL',
         'Collection of duplex rental units with long-term tenants.');

    INSERT INTO property_ownerships
        (property_id, owner_party_id, role, ownership_percentage, effective_from, effective_to, notes)
    VALUES
        -- Maple Grove Apartments (shared ownership)
        (1, 'user-1001', 'PRIMARY_OWNER', 60.00, DATE '2022-05-01', NULL, 'Managing owner.'),
        (1, 'user-1002', 'CO_OWNER',      40.00, DATE '2022-05-01', NULL, NULL),

        -- Downtown Office Suites (org-owned)
        (2, 'org-2005',  'PRIMARY_OWNER', 100.00, DATE '2021-09-15', NULL, 'Operated by Horizon IT Solutions.'),

        -- Blue Ridge Storage (individual owner)
        (3, 'user-1003', 'PRIMARY_OWNER', 100.00, DATE '2020-03-10', NULL, NULL),

        -- Riverbend Retail Center (mixed ownership)
        (4, 'org-2002',  'PRIMARY_OWNER', 70.00, DATE '2019-11-20', NULL, 'Lead investor.'),
        (4, 'user-1004', 'CO_OWNER',      30.00, DATE '2019-11-20', NULL, NULL),

        -- Cedar Hill Duplexes (family-style split)
        (5, 'user-1005', 'PRIMARY_OWNER', 50.00, DATE '2023-01-01', NULL, NULL),
        (5, 'user-1006', 'CO_OWNER',      50.00, DATE '2023-01-01', NULL, 'Joint investment property.');
END IF;
END $$;

