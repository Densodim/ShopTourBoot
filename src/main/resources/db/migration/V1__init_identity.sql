-- V1: identity + session foundation for Voyage API
CREATE TABLE app_user (
    id                  UUID PRIMARY KEY,
    email               VARCHAR(320) NOT NULL,
    password_hash       VARCHAR(255) NOT NULL,
    display_name        VARCHAR(120) NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL,
    updated_at          TIMESTAMPTZ NOT NULL,
    deleted_at          TIMESTAMPTZ NULL
);

CREATE UNIQUE INDEX uk_app_user_email ON app_user (email) WHERE deleted_at IS NULL;

CREATE TABLE refresh_token (
    id                  UUID PRIMARY KEY,
    user_id             UUID NOT NULL REFERENCES app_user (id),
    token_hash          VARCHAR(64) NOT NULL,
    device_name         VARCHAR(120) NULL,
    expires_at          TIMESTAMPTZ NOT NULL,
    revoked_at          TIMESTAMPTZ NULL,
    created_at          TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX uk_refresh_token_hash ON refresh_token (token_hash);
CREATE INDEX idx_refresh_token_user ON refresh_token (user_id);

CREATE TABLE idempotency_record (
    id                  UUID PRIMARY KEY,
    user_id             UUID NOT NULL,
    route_key           VARCHAR(255) NOT NULL,
    idempotency_key     VARCHAR(64) NOT NULL,
    request_hash        VARCHAR(64) NOT NULL,
    response_status     INT NOT NULL,
    response_body       TEXT NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL,
    expires_at          TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX uk_idempotency_user_route_key
    ON idempotency_record (user_id, route_key, idempotency_key);
