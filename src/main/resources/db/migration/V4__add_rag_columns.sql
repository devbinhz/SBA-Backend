ALTER TABLE books
    ADD COLUMN file_key VARCHAR(500),
    ADD COLUMN cover_key VARCHAR(500),
    ADD COLUMN last_indexed_at TIMESTAMPTZ;
