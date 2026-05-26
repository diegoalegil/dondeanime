ALTER TABLE anime
    ADD COLUMN IF NOT EXISTS search_vector tsvector
    GENERATED ALWAYS AS (
        setweight(to_tsvector('spanish', coalesce(title_english, '')), 'A') ||
        setweight(to_tsvector('spanish', coalesce(title_romaji, '')), 'A') ||
        setweight(to_tsvector('simple', coalesce(slug, '')), 'B') ||
        setweight(to_tsvector('spanish', coalesce(description_es, '')), 'C') ||
        setweight(to_tsvector('english', coalesce(description, '')), 'D')
    ) STORED;

CREATE INDEX IF NOT EXISTS idx_anime_search_vector
    ON anime USING GIN (search_vector);
