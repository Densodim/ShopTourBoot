CREATE INDEX idx_purchase_trip_keyset
	ON purchase (trip_id, purchase_date DESC, id DESC)
	WHERE deleted_at IS NULL;
