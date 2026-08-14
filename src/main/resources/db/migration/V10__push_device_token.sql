-- Store the live FCM token so the API can send. token_hash stays the lookup key.
ALTER TABLE push_device
	ADD COLUMN token VARCHAR(512) NULL;
