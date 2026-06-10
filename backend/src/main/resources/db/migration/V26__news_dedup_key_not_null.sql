-- V24 backfilleó dedup_key (= source_url para filas previas) y la ingesta
-- siempre lo rellena: la invariante ya se cumple, hazla explícita.
UPDATE public.news_item SET dedup_key = source_url WHERE dedup_key IS NULL;
ALTER TABLE public.news_item ALTER COLUMN dedup_key SET NOT NULL;
