-- Create knowledge base document table
-- Stores documents in three scopes: GLOBAL (shared), PROGRAM (versioned), USER (per-user memory)
CREATE TABLE IF NOT EXISTS kb_document (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    scope TEXT NOT NULL,             -- GLOBAL | PROGRAM | USER
    user_id TEXT NULL,               -- required when scope = USER, null otherwise
    source TEXT NOT NULL,            -- handbook | faq | onboarding | walk_summary | program_template | exercise
    title TEXT NOT NULL,
    content TEXT NOT NULL,
    tags TEXT[] NULL,                -- array of tags for additional filtering
    version INT NOT NULL DEFAULT 1,  -- version number for PROGRAM content
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    
    -- Constraints
    CONSTRAINT kb_document_scope_check CHECK (scope IN ('GLOBAL', 'PROGRAM', 'USER')),
    CONSTRAINT kb_document_user_scope_check CHECK (
        (scope = 'USER' AND user_id IS NOT NULL) OR 
        (scope != 'USER' AND user_id IS NULL)
    )
);

-- Index for efficient scope + user filtering
CREATE INDEX IF NOT EXISTS idx_kb_document_scope_user
    ON kb_document(scope, user_id);

-- Index for source-based queries
CREATE INDEX IF NOT EXISTS idx_kb_document_source
    ON kb_document(source);

-- Index for version-based queries (PROGRAM content)
CREATE INDEX IF NOT EXISTS idx_kb_document_version
    ON kb_document(version);


-- Create knowledge base chunk table
-- Stores text chunks with vector embeddings for similarity search
-- Using dimension 3072 for OpenAI text-embedding-3-large model
CREATE TABLE IF NOT EXISTS kb_chunk (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID NOT NULL REFERENCES kb_document(id) ON DELETE CASCADE,
    chunk_index INT NOT NULL,        -- chunk position within the document (0-based)
    chunk_text TEXT NOT NULL,        -- the actual text chunk
    embedding vector(3072) NOT NULL, -- vector embedding (dimension 3072)
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    
    -- Ensure unique chunk index per document
    CONSTRAINT kb_chunk_unique_index UNIQUE (document_id, chunk_index)
);

-- Index for document-based queries
CREATE INDEX IF NOT EXISTS idx_kb_chunk_doc
    ON kb_chunk(document_id);

-- HNSW index for fast approximate nearest neighbor search using cosine similarity
-- This is the key index for vector similarity search
CREATE INDEX IF NOT EXISTS idx_kb_chunk_embedding_hnsw
    ON kb_chunk USING hnsw (embedding vector_cosine_ops);

-- GIN index for full-text search on chunk text (optional but useful)
CREATE INDEX IF NOT EXISTS idx_kb_chunk_text_gin
    ON kb_chunk USING gin (to_tsvector('english', chunk_text));


-- Optional: Add KB references to conversation for traceability
-- Stores which KB chunks were used during a conversation
ALTER TABLE conversation
    ADD COLUMN IF NOT EXISTS kb_refs_json JSONB NULL;

-- Index for JSON queries on KB references
CREATE INDEX IF NOT EXISTS idx_conversation_kb_refs
    ON conversation USING gin (kb_refs_json);

-- Add comment explaining the kb_refs_json structure
COMMENT ON COLUMN conversation.kb_refs_json IS 
'JSON array of KB chunk IDs that were retrieved and used during this conversation. 
Example: [{"chunk_id": "uuid", "relevance_score": 0.85, "retrieved_at": "timestamp"}]';
