-- One-time tokens for POST /api/auth/reset-password. Hashed at rest.
CREATE TABLE password_reset_token (
	id           UUID PRIMARY KEY,
	user_id      UUID NOT NULL REFERENCES app_user (id),
	token_hash   VARCHAR(64) NOT NULL,
	expires_at   TIMESTAMPTZ NOT NULL,
	used_at      TIMESTAMPTZ NULL,
	created_at   TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX uk_password_reset_token_hash ON password_reset_token (token_hash);
CREATE INDEX idx_password_reset_token_user ON password_reset_token (user_id);
