PRAGMA foreign_keys = ON;

CREATE TABLE users (
  id TEXT PRIMARY KEY,
  username TEXT NOT NULL UNIQUE COLLATE NOCASE,
  display_name TEXT NOT NULL,
  pin_hash TEXT NOT NULL,
  avatar_initials TEXT NOT NULL,
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL
);

CREATE TABLE devices (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL REFERENCES users(id),
  device_name TEXT NOT NULL,
  refresh_token_hash TEXT,
  fcm_token TEXT,
  last_seen_at TEXT NOT NULL,
  created_at TEXT NOT NULL,
  revoked_at TEXT
);

CREATE TABLE categories (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL UNIQUE COLLATE NOCASE,
  icon TEXT NOT NULL DEFAULT 'category',
  active INTEGER NOT NULL DEFAULT 1,
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL,
  deleted_at TEXT,
  version INTEGER NOT NULL DEFAULT 1,
  updated_by TEXT REFERENCES users(id)
);

CREATE TABLE accounts (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  bank_name TEXT,
  last4 TEXT,
  owner_user_id TEXT REFERENCES users(id),
  payment_method TEXT NOT NULL CHECK(payment_method IN ('CASH','UPI','OTHER')),
  active INTEGER NOT NULL DEFAULT 1,
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL,
  deleted_at TEXT,
  version INTEGER NOT NULL DEFAULT 1,
  updated_by TEXT REFERENCES users(id)
);

CREATE TABLE transactions (
  id TEXT PRIMARY KEY,
  amount_paise INTEGER NOT NULL CHECK(amount_paise > 0),
  currency TEXT NOT NULL DEFAULT 'INR' CHECK(currency = 'INR'),
  category_id TEXT REFERENCES categories(id),
  paid_by_user_id TEXT NOT NULL REFERENCES users(id),
  payment_method TEXT NOT NULL CHECK(payment_method IN ('CASH','UPI','OTHER')),
  account_id TEXT REFERENCES accounts(id),
  merchant TEXT,
  note TEXT,
  occurred_at TEXT NOT NULL,
  source TEXT NOT NULL CHECK(source IN ('MANUAL','SMS','NOTIFICATION')),
  status TEXT NOT NULL CHECK(status IN ('DETECTED','CONFIRMED','IGNORED','DELETED')),
  source_reference TEXT,
  fingerprint TEXT,
  created_by_user_id TEXT NOT NULL REFERENCES users(id),
  updated_by_user_id TEXT NOT NULL REFERENCES users(id),
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL,
  deleted_at TEXT,
  version INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE merchant_rules (
  id TEXT PRIMARY KEY,
  merchant_pattern TEXT NOT NULL UNIQUE COLLATE NOCASE,
  category_id TEXT NOT NULL REFERENCES categories(id),
  created_by_user_id TEXT NOT NULL REFERENCES users(id),
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL,
  deleted_at TEXT,
  version INTEGER NOT NULL DEFAULT 1,
  updated_by TEXT REFERENCES users(id)
);

CREATE INDEX idx_transactions_updated ON transactions(updated_at);
CREATE INDEX idx_transactions_occurred ON transactions(occurred_at DESC);
CREATE INDEX idx_transactions_status ON transactions(status);
CREATE INDEX idx_transactions_fingerprint ON transactions(fingerprint);
CREATE INDEX idx_devices_user ON devices(user_id);

