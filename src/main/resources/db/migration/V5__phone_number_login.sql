-- Add name column (use username value as initial name for existing rows)
ALTER TABLE users ADD COLUMN IF NOT EXISTS name VARCHAR(100);
UPDATE users SET name = username WHERE name IS NULL;
ALTER TABLE users ALTER COLUMN name SET NOT NULL;

-- Ensure phone_number column exists (may already exist from Hibernate auto-update)
ALTER TABLE users ADD COLUMN IF NOT EXISTS phone_number VARCHAR(20);

-- Remove users that have no phone_number (dev only — admin will be re-seeded by bootstrap)
DELETE FROM delivery_tasks WHERE assigned_agent_id IN (SELECT id FROM users WHERE phone_number IS NULL OR phone_number = '');
DELETE FROM delivery_tasks WHERE assigned_by_id IN (SELECT id FROM users WHERE phone_number IS NULL OR phone_number = '');
DELETE FROM harvests WHERE farmer_id IN (SELECT id FROM users WHERE phone_number IS NULL OR phone_number = '');
DELETE FROM demands WHERE retailer_id IN (SELECT id FROM users WHERE phone_number IS NULL OR phone_number = '');
DELETE FROM users WHERE phone_number IS NULL OR phone_number = '';

-- Make phone_number NOT NULL and unique
ALTER TABLE users ALTER COLUMN phone_number SET NOT NULL;
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_phone_number_key;
ALTER TABLE users ADD CONSTRAINT users_phone_number_unique UNIQUE (phone_number);

-- Drop username column
ALTER TABLE users DROP COLUMN IF EXISTS username;

-- Update revoked_users: rename username -> phone_number
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='revoked_users' AND column_name='username') THEN
        ALTER TABLE revoked_users RENAME COLUMN username TO phone_number;
        ALTER TABLE revoked_users ALTER COLUMN phone_number TYPE VARCHAR(20);
    END IF;
END $$;

-- Update login_attempts: rename username -> phone_number
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='login_attempts' AND column_name='username') THEN
        ALTER TABLE login_attempts RENAME COLUMN username TO phone_number;
        ALTER TABLE login_attempts ALTER COLUMN phone_number TYPE VARCHAR(20);
    END IF;
END $$;
