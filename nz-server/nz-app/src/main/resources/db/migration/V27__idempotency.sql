CREATE TABLE IF NOT EXISTS nz_idempotency (
    scope VARCHAR(512) NOT NULL,
    request_key VARCHAR(128) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    response_json TEXT,
    expires_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY(scope, request_key)
);
CREATE INDEX IF NOT EXISTS idx_nz_idempotency_expiry ON nz_idempotency(expires_at);
