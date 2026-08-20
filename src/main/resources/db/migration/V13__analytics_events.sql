-- V13: first-party product analytics ingest (POST /api/me/analytics-events)
CREATE TABLE analytics_event (
	id                  UUID PRIMARY KEY,
	user_id             UUID NOT NULL REFERENCES app_user (id),
	client_event_id     VARCHAR(64) NOT NULL,
	name                VARCHAR(120) NOT NULL,
	properties          TEXT NOT NULL DEFAULT '{}',
	occurred_at         TIMESTAMPTZ NOT NULL,
	received_at         TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX uk_analytics_event_user_client
	ON analytics_event (user_id, client_event_id);

CREATE INDEX idx_analytics_event_user_received
	ON analytics_event (user_id, received_at DESC);
