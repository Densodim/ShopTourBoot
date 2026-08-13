-- V5: diary entries and wishlist items
CREATE TABLE diary_entry (
	id              UUID PRIMARY KEY,
	trip_id         UUID NOT NULL REFERENCES trip (id),
	entry_date      DATE NOT NULL,
	mood            VARCHAR(8) NOT NULL,
	text            VARCHAR(4000) NOT NULL,
	created_at      TIMESTAMPTZ NOT NULL,
	updated_at      TIMESTAMPTZ NOT NULL,
	deleted_at      TIMESTAMPTZ NULL
);

CREATE INDEX idx_diary_trip_live ON diary_entry (trip_id, entry_date) WHERE deleted_at IS NULL;

CREATE TABLE wishlist_item (
	id                  UUID PRIMARY KEY,
	user_id             UUID NOT NULL REFERENCES app_user (id),
	name                VARCHAR(200) NOT NULL,
	city                VARCHAR(120) NOT NULL,
	target_amount       NUMERIC(19, 4) NOT NULL,
	target_currency     VARCHAR(3) NOT NULL,
	icon_emoji          VARCHAR(8) NULL,
	note                VARCHAR(500) NULL,
	created_at          TIMESTAMPTZ NOT NULL,
	updated_at          TIMESTAMPTZ NOT NULL,
	deleted_at          TIMESTAMPTZ NULL
);

CREATE INDEX idx_wishlist_user_live ON wishlist_item (user_id) WHERE deleted_at IS NULL;
