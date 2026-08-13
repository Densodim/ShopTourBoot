-- V6: media, export jobs, push devices, trip invites
CREATE TABLE media_asset (
	id              UUID PRIMARY KEY,
	user_id         UUID NOT NULL REFERENCES app_user (id),
	purpose         VARCHAR(16) NOT NULL,
	status          VARCHAR(16) NOT NULL,
	content_type    VARCHAR(128) NOT NULL,
	byte_size       BIGINT NOT NULL,
	sha256_hex      VARCHAR(64) NULL,
	created_at      TIMESTAMPTZ NOT NULL,
	updated_at      TIMESTAMPTZ NOT NULL,
	deleted_at      TIMESTAMPTZ NULL
);

CREATE INDEX idx_media_asset_user ON media_asset (user_id) WHERE deleted_at IS NULL;

CREATE TABLE export_job (
	id                  UUID PRIMARY KEY,
	trip_id             UUID NOT NULL REFERENCES trip (id),
	owner_id            UUID NOT NULL REFERENCES app_user (id),
	format              VARCHAR(8) NOT NULL,
	status              VARCHAR(16) NOT NULL,
	include_tax_free    BOOLEAN NOT NULL,
	include_diary       BOOLEAN NOT NULL,
	download_url        VARCHAR(512) NULL,
	error_code          VARCHAR(64) NULL,
	created_at          TIMESTAMPTZ NOT NULL,
	finished_at         TIMESTAMPTZ NULL,
	expires_at          TIMESTAMPTZ NULL
);

CREATE INDEX idx_export_job_owner ON export_job (owner_id);

CREATE TABLE push_device (
	id              UUID PRIMARY KEY,
	user_id         UUID NOT NULL REFERENCES app_user (id),
	token_hash      VARCHAR(64) NOT NULL,
	platform        VARCHAR(16) NOT NULL,
	app_version     VARCHAR(64) NULL,
	device_name     VARCHAR(120) NULL,
	created_at      TIMESTAMPTZ NOT NULL,
	last_seen_at    TIMESTAMPTZ NOT NULL,
	deleted_at      TIMESTAMPTZ NULL
);

CREATE UNIQUE INDEX uk_push_device_user_token
	ON push_device (user_id, token_hash) WHERE deleted_at IS NULL;

CREATE TABLE trip_invite (
	id                      UUID PRIMARY KEY,
	trip_id                 UUID NOT NULL REFERENCES trip (id),
	email                   VARCHAR(320) NOT NULL,
	display_name_hint       VARCHAR(60) NULL,
	status                  VARCHAR(16) NOT NULL,
	created_at              TIMESTAMPTZ NOT NULL,
	expires_at              TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_trip_invite_trip ON trip_invite (trip_id);
