CREATE TABLE agent_prompt (
    agent_id UUID PRIMARY KEY REFERENCES exam_agent(id),
    prompt TEXT NOT NULL DEFAULT '' CHECK (length(prompt) <= 5000),
    revision BIGINT NOT NULL DEFAULT 0 CHECK (revision >= 0),
    updated_at TIMESTAMPTZ NOT NULL
);
INSERT INTO agent_prompt (agent_id, updated_at) SELECT id, created_at FROM exam_agent;
