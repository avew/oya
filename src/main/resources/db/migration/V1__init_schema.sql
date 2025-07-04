-- V1__init_schema.sql
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS vector;

-- Table for document metadata
CREATE TABLE document (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    upload_path VARCHAR(500) NOT NULL,
    status VARCHAR(50) DEFAULT 'PROCESSING',
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()
);

-- Table for document chunks with vector embeddings
CREATE TABLE document_chunk (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    document_id UUID NOT NULL REFERENCES document(id) ON DELETE CASCADE,
    chunk_index INTEGER NOT NULL,
    content TEXT NOT NULL,
    embedding vector(1536), -- OpenAI ada-002 embedding dimension
    token_count INTEGER DEFAULT 0,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(),
    UNIQUE(document_id, chunk_index)
);

-- Indexes for better performance
CREATE INDEX idx_document_status ON document (status);
CREATE INDEX idx_document_created_at ON document (created_at);
CREATE INDEX idx_document_chunk_document_id ON document_chunk (document_id);
CREATE INDEX idx_document_chunk_content ON document_chunk USING GIN (to_tsvector('english', content));

-- Vector similarity search indexes
CREATE INDEX idx_document_chunk_embedding_cosine ON document_chunk USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
CREATE INDEX idx_document_chunk_embedding_l2 ON document_chunk USING ivfflat (embedding vector_l2_ops) WITH (lists = 100);

CREATE TABLE chat_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    response TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_chat_history_user_id ON chat_history (user_id);
CREATE INDEX idx_chat_history_created_at ON chat_history (created_at);
