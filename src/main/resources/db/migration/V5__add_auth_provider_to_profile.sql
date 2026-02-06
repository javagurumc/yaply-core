-- Add auth_provider column to profile table
ALTER TABLE profile
    ADD COLUMN IF NOT EXISTS auth_provider TEXT NOT NULL DEFAULT 'EMAIL';

-- Add constraints for provider fields
ALTER TABLE profile
    ADD COLUMN IF NOT EXISTS provider_user_id TEXT,
    ADD COLUMN IF NOT EXISTS provider_email TEXT;
