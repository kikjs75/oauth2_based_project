-- Expand username column to accommodate email addresses from OAuth2 providers
ALTER TABLE users ALTER COLUMN username TYPE VARCHAR(255);

-- Make password nullable for OAuth2 users who don't have a local password
ALTER TABLE users ALTER COLUMN password DROP NOT NULL;

-- Add OAuth2 provider columns
ALTER TABLE users ADD COLUMN provider    VARCHAR(20)  NOT NULL DEFAULT 'LOCAL';
ALTER TABLE users ADD COLUMN provider_id VARCHAR(255) NULL;
ALTER TABLE users ADD CONSTRAINT uk_users_provider UNIQUE (provider, provider_id);
