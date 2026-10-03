-- Keep the client edit time for conflict resolution and use server time for pull cursors.
ALTER TABLE categories ADD COLUMN client_updated_at TEXT;
ALTER TABLE accounts ADD COLUMN client_updated_at TEXT;
ALTER TABLE transactions ADD COLUMN client_updated_at TEXT;
ALTER TABLE merchant_rules ADD COLUMN client_updated_at TEXT;

UPDATE categories SET client_updated_at = updated_at;
UPDATE accounts SET client_updated_at = updated_at;
UPDATE transactions SET client_updated_at = updated_at;
UPDATE merchant_rules SET client_updated_at = updated_at;

-- Send the current catalog and history once to every client after the migration.
UPDATE categories SET updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now');
UPDATE accounts SET updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now');
UPDATE transactions SET updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now');
UPDATE merchant_rules SET updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now');
