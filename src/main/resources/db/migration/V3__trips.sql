-- V3: trips and travelers
CREATE TABLE trip (
	id                          UUID PRIMARY KEY,
	owner_id                    UUID NOT NULL REFERENCES app_user (id),
	city                        VARCHAR(120) NOT NULL,
	country                     VARCHAR(120) NOT NULL,
	country_code                VARCHAR(2) NULL,
	flag_emoji                  VARCHAR(16) NULL,
	status                      VARCHAR(16) NOT NULL,
	start_date                  DATE NOT NULL,
	end_date                    DATE NOT NULL,
	budget_amount               NUMERIC(19, 4) NOT NULL,
	budget_currency             VARCHAR(3) NOT NULL,
	default_vat_rate_percent    NUMERIC(5, 2) NOT NULL DEFAULT 0,
	fx_trip_currency            VARCHAR(3) NULL,
	fx_quote_currency           VARCHAR(3) NULL,
	fx_rate                     NUMERIC(19, 8) NULL,
	fx_rate_date                DATE NULL,
	fx_provider                 VARCHAR(64) NULL,
	created_at                  TIMESTAMPTZ NOT NULL,
	updated_at                  TIMESTAMPTZ NOT NULL,
	deleted_at                  TIMESTAMPTZ NULL
);

CREATE INDEX idx_trip_owner_live ON trip (owner_id) WHERE deleted_at IS NULL;

CREATE TABLE traveler (
	id              UUID PRIMARY KEY,
	trip_id         UUID NOT NULL REFERENCES trip (id),
	user_id         UUID NULL REFERENCES app_user (id),
	name            VARCHAR(60) NOT NULL,
	color_hex       VARCHAR(7) NOT NULL,
	avatar_glyph    VARCHAR(2) NULL,
	is_owner        BOOLEAN NOT NULL DEFAULT FALSE,
	created_at      TIMESTAMPTZ NOT NULL,
	deleted_at      TIMESTAMPTZ NULL
);

CREATE INDEX idx_traveler_trip ON traveler (trip_id);
