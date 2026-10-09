CREATE TABLE issue_reports (
  id TEXT PRIMARY KEY,
  reporter_user_id TEXT NOT NULL REFERENCES users(id),
  category TEXT NOT NULL CHECK(category IN ('BUG','FEATURE','OTHER')),
  title TEXT NOT NULL,
  description TEXT NOT NULL,
  status TEXT NOT NULL DEFAULT 'OPEN' CHECK(status IN ('OPEN','IN_PROGRESS','RESOLVED')),
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL
);

CREATE TABLE issue_screenshots (
  id TEXT PRIMARY KEY,
  issue_id TEXT NOT NULL REFERENCES issue_reports(id) ON DELETE CASCADE,
  mime_type TEXT NOT NULL CHECK(mime_type IN ('image/jpeg','image/png')),
  data_base64 TEXT NOT NULL,
  created_at TEXT NOT NULL
);

CREATE INDEX idx_issue_reports_created ON issue_reports(created_at DESC);
CREATE INDEX idx_issue_screenshots_issue ON issue_screenshots(issue_id);
