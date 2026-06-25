-- pgvector IVFFlat index on empty table can fail on some hosts; defer indexing
DROP INDEX IF EXISTS idx_knowledge_embedding;
