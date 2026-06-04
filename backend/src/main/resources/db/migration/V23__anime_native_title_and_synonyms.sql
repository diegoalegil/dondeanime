-- Native (Japanese) title and AniList synonyms.
-- These feed the AniList <-> TMDb title matcher so that synonym and
-- native-title matches (a primary source of misses today) can be found.

ALTER TABLE anime ADD COLUMN title_native VARCHAR(255);

-- Mirrors the anime_genre element-collection table: composite primary key,
-- rows removed automatically when the owning anime is deleted.
CREATE TABLE anime_synonym (
    anime_id BIGINT       NOT NULL REFERENCES anime (id) ON DELETE CASCADE,
    synonym  VARCHAR(255) NOT NULL,
    PRIMARY KEY (anime_id, synonym)
);
