-- Local pre-signed upload bodies (S3 later). Postgres BYTEA.
ALTER TABLE media_asset
	ADD COLUMN content BYTEA NULL;
