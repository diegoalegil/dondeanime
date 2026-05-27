CREATE TABLE IF NOT EXISTS curated_list (
    id BIGSERIAL PRIMARY KEY,
    slug VARCHAR(180) NOT NULL,
    title VARCHAR(160) NOT NULL,
    description TEXT NOT NULL,
    owner VARCHAR(120) NOT NULL,
    visibility VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_curated_list_slug UNIQUE (slug),
    CONSTRAINT chk_curated_list_visibility CHECK (visibility IN ('PUBLIC', 'UNLISTED', 'PRIVATE')),
    CONSTRAINT chk_curated_list_status CHECK (status IN ('DRAFT', 'PENDING_REVIEW', 'PUBLISHED', 'REJECTED'))
);

CREATE TABLE IF NOT EXISTS curated_list_item (
    id BIGSERIAL PRIMARY KEY,
    curated_list_id BIGINT NOT NULL,
    anime_slug VARCHAR(180) NOT NULL,
    position INTEGER NOT NULL,
    note TEXT,
    CONSTRAINT fk_curated_list_item_list
        FOREIGN KEY (curated_list_id)
        REFERENCES curated_list (id)
        ON DELETE CASCADE,
    CONSTRAINT uk_curated_list_item_anime UNIQUE (curated_list_id, anime_slug),
    CONSTRAINT uk_curated_list_item_position UNIQUE (curated_list_id, position),
    CONSTRAINT chk_curated_list_item_position CHECK (position >= 1)
);

CREATE INDEX IF NOT EXISTS idx_curated_list_status_visibility
    ON curated_list (status, visibility);

CREATE INDEX IF NOT EXISTS idx_curated_list_owner
    ON curated_list (owner);

CREATE INDEX IF NOT EXISTS idx_curated_list_item_list_position
    ON curated_list_item (curated_list_id, position);

CREATE INDEX IF NOT EXISTS idx_curated_list_item_anime_slug
    ON curated_list_item (anime_slug);
