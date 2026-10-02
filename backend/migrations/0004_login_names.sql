-- Preserve IDs and transaction ownership; change only the login identifiers.
UPDATE users SET username = 'alpa', updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now') WHERE id = 'user-mom';
UPDATE users SET username = 'hitesh', updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now') WHERE id = 'user-dad';
