CREATE TABLE exam_agent (
    id UUID PRIMARY KEY,
    owner_profile_id UUID NOT NULL REFERENCES profile(id),
    name VARCHAR(120) NOT NULL CHECK (length(btrim(name)) > 0),
    description VARCHAR(2000) NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_exam_agent_owner_created ON exam_agent(owner_profile_id, created_at DESC, id);
