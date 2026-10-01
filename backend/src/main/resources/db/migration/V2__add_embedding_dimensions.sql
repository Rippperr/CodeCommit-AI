ALTER TABLE embeddings
    ADD COLUMN dimensions INTEGER;

UPDATE embeddings
SET dimensions = 1536
WHERE dimensions IS NULL;

ALTER TABLE embeddings
    ALTER COLUMN dimensions SET NOT NULL;
