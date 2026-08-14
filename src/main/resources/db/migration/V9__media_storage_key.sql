-- Object-store key for media bytes (S3 or local disk). BYTEA stays as a read fallback.
ALTER TABLE media_asset
	ADD COLUMN storage_key VARCHAR(512) NULL;
