CREATE TABLE IF NOT EXISTS activations (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  email TEXT NOT NULL UNIQUE,
  status TEXT NOT NULL DEFAULT 'PENDING',
  requested_at INTEGER,
  approved_at INTEGER,
  activation_start_date INTEGER,
  expiration_date INTEGER,
  duration_days INTEGER,
  device_id TEXT,
  updated_at INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_activations_status ON activations(status);

-- Existing D1 databases created before device binding also need:
-- ALTER TABLE activations ADD COLUMN device_id TEXT;
