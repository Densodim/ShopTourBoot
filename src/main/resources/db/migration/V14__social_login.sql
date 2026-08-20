-- Federated Google / Apple accounts: social-only users have no password.
ALTER TABLE app_user
    ALTER COLUMN password_hash DROP NOT NULL;

ALTER TABLE app_user
    ADD COLUMN google_sub VARCHAR(255) NULL,
    ADD COLUMN apple_sub VARCHAR(255) NULL;

CREATE UNIQUE INDEX uk_app_user_google_sub
    ON app_user (google_sub)
    WHERE google_sub IS NOT NULL AND deleted_at IS NULL;

CREATE UNIQUE INDEX uk_app_user_apple_sub
    ON app_user (apple_sub)
    WHERE apple_sub IS NOT NULL AND deleted_at IS NULL;
