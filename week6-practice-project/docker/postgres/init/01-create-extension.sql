-- docker/postgres/init/01-create-extension.sql

-- 1. DB 선택
\connect app_vector_db;

-- 2. pgvector extension 생성
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS hstore;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 3. VectorStore 대화 기억용 테이블 생성
CREATE TABLE IF NOT EXISTS chat_memory_vector_store (
    id uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
    content text,
    metadata json,
    embedding vector(1536)
);

CREATE INDEX IF NOT EXISTS chat_memory_vector_store_embedding_idx
    ON chat_memory_vector_store
    USING hnsw (embedding vector_cosine_ops);
