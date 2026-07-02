BEGIN;

ALTER TABLE credentials
    ADD COLUMN name TEXT;

UPDATE credentials
SET name = 'Default credential'
WHERE name IS NULL;

ALTER TABLE credentials
    ALTER COLUMN name SET NOT NULL;

COMMIT;