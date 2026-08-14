-- Persist the upload window returned as uploadExpiresAt.
ALTER TABLE media_asset
	ADD COLUMN upload_expires_at TIMESTAMPTZ NULL;
