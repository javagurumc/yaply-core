-- V1__init.sql
-- Core tables for Clarity Walk

CREATE TABLE IF NOT EXISTS conversation (
    id UUID PRIMARY KEY,
    user_id TEXT NOT NULL,
    started_at TIMESTAMPTZ NOT NULL,
    ended_at TIMESTAMPTZ NULL,
    status TEXT NOT NULL,
    session_config_json TEXT NULL,
    final_summary_json TEXT NULL
);

-- Helps query "recent conversations by user"
CREATE INDEX IF NOT EXISTS idx_conversation_user_started
    ON conversation (user_id, started_at DESC);

CREATE TABLE IF NOT EXISTS conversation_event (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL REFERENCES conversation(id) ON DELETE CASCADE,
    ts TIMESTAMPTZ NOT NULL,
    role TEXT NOT NULL,  -- user|assistant|tool|system
    type TEXT NOT NULL,  -- text|transcript|tool_output|...
    content TEXT NOT NULL
    );

-- Fast load transcript in order
CREATE INDEX IF NOT EXISTS idx_conv_event_conv_ts
    ON conversation_event (conversation_id, ts ASC);

-- Optional: quick filtering by role/type (handy for summaries)
CREATE INDEX IF NOT EXISTS idx_conv_event_conv_role_type
    ON conversation_event (conversation_id, role, type);
