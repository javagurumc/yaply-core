-- V6__add_custom_tutor_prompt.sql
-- Add custom tutor prompt field to profile table

ALTER TABLE profile
    ADD COLUMN IF NOT EXISTS custom_tutor_prompt TEXT DEFAULT '',
    ADD COLUMN IF NOT EXISTS custom_tutor_prompt_updated_at TIMESTAMPTZ DEFAULT NOW();

-- Create index for faster lookups if needed
CREATE INDEX IF NOT EXISTS idx_profile_custom_prompt_updated
    ON profile (custom_tutor_prompt_updated_at DESC);

