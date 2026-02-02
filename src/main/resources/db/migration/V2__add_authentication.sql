-- V2__add_authentication.sql
-- Add authentication fields to profile table

-- Add password_hash column for storing BCrypt hashed passwords
ALTER TABLE profile ADD COLUMN password_hash TEXT;

-- Add unique constraint on email for login
ALTER TABLE profile ADD CONSTRAINT uk_profile_email UNIQUE (email);

-- Add index on email for faster lookups during login
CREATE INDEX IF NOT EXISTS idx_profile_email ON profile (email);
