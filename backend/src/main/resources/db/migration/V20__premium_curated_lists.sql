ALTER TABLE curated_list
    ADD COLUMN premium_only BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE curated_list_metric_event (
    id BIGSERIAL PRIMARY KEY,
    list_slug VARCHAR(180) NOT NULL,
    anime_slug VARCHAR(180),
    event_type VARCHAR(30) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_curated_list_metric_event_type_time
    ON curated_list_metric_event (event_type, occurred_at);

CREATE INDEX idx_curated_list_metric_event_list_time
    ON curated_list_metric_event (list_slug, occurred_at);
