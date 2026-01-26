CREATE TABLE IF NOT EXISTS profile (
    id UUID PRIMARY KEY,
    user_id TEXT NOT NULL,
    email TEXT NOT NULL,
    responses TEXT NOT NULL
);
