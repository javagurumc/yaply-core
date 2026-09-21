CREATE TABLE tutor_api_credential (
 owner_profile_id UUID PRIMARY KEY REFERENCES profile(id),
 ciphertext BYTEA NOT NULL,
 nonce BYTEA NOT NULL CHECK (octet_length(nonce) = 12),
 encryption_key_id VARCHAR(100) NOT NULL,
 format_version INTEGER NOT NULL,
 updated_at TIMESTAMPTZ NOT NULL
);
