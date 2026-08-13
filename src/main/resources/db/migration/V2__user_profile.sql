-- V2: profile fields the /me and auth contracts need on app_user
ALTER TABLE app_user
	ADD COLUMN locale VARCHAR(5) NOT NULL DEFAULT 'ru',
	ADD COLUMN preferred_currency VARCHAR(3) NOT NULL DEFAULT 'RUB',
	ADD COLUMN theme VARCHAR(16) NOT NULL DEFAULT 'SYSTEM',
	ADD COLUMN push_notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE,
	ADD COLUMN dark_mode BOOLEAN NOT NULL DEFAULT FALSE,
	ADD COLUMN avatar_media_id UUID NULL,
	ADD COLUMN premium_plan VARCHAR(16) NOT NULL DEFAULT 'FREE';
