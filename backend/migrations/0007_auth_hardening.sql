-- Keep login limits consistent across Worker instances.
CREATE TABLE IF NOT EXISTS auth_ip_attempts (
  key TEXT PRIMARY KEY,
  attempts INTEGER NOT NULL,
  window_started_at INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS auth_ip_attempts_window_idx ON auth_ip_attempts(window_started_at);

CREATE TABLE IF NOT EXISTS auth_login_failures (
  user_id TEXT PRIMARY KEY REFERENCES users(id),
  attempts INTEGER NOT NULL,
  window_started_at INTEGER NOT NULL,
  locked_at TEXT
);

-- Historical development credentials must never remain usable after deployment.
-- Local development can explicitly provision its own users after migrations.
UPDATE users SET pin_hash = 'disabled:development-seed', updated_at = datetime('now')
WHERE pin_hash = 'pbkdf2:310000:ZGV2LXNlZWQtc2FsdA==:qUFePm/yPr+fglRAeliwzX+paYRTjM7ALeoDxIaJQm0=';
