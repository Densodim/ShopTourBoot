-- V4: purchases and equal splits
CREATE TABLE purchase (
	id                      UUID PRIMARY KEY,
	trip_id                 UUID NOT NULL REFERENCES trip (id),
	name                    VARCHAR(200) NOT NULL,
	category                VARCHAR(32) NOT NULL,
	gross_amount            NUMERIC(19, 4) NOT NULL,
	currency                VARCHAR(3) NOT NULL,
	net_amount              NUMERIC(19, 4) NOT NULL,
	vat_amount              NUMERIC(19, 4) NOT NULL,
	vat_rate_percent        NUMERIC(5, 2) NOT NULL,
	vat_included            BOOLEAN NOT NULL,
	tax_refund_eligible     BOOLEAN NOT NULL DEFAULT FALSE,
	place                   VARCHAR(200) NULL,
	purchase_date           DATE NOT NULL,
	purchase_time           TIME NOT NULL,
	receipt_media_id        UUID NULL,
	created_at              TIMESTAMPTZ NOT NULL,
	updated_at              TIMESTAMPTZ NOT NULL,
	deleted_at              TIMESTAMPTZ NULL
);

CREATE INDEX idx_purchase_trip_live ON purchase (trip_id, purchase_date) WHERE deleted_at IS NULL;

CREATE TABLE purchase_split (
	purchase_id     UUID NOT NULL REFERENCES purchase (id),
	traveler_id     UUID NOT NULL REFERENCES traveler (id),
	PRIMARY KEY (purchase_id, traveler_id)
);
