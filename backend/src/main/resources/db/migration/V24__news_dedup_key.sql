-- Deduplicación robusta de noticias: clave canónica (URL normalizada) +
-- hash de contenido, calculados por la librería anime-feed-parser.
-- source_url sigue siendo unique (barrera para filas ya existentes); dedup_key
-- añade la dedup canónica que source_url no pillaba (utm_*, barra final, http/https).
-- Idempotente (re-ejecutable), mismo patrón DO $$ ... IF NOT EXISTS que V22.

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'news_item' AND column_name = 'dedup_key'
    ) THEN
        ALTER TABLE public.news_item ADD COLUMN dedup_key character varying(800);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'news_item' AND column_name = 'content_hash'
    ) THEN
        ALTER TABLE public.news_item ADD COLUMN content_hash character varying(64);
    END IF;
END $$;

-- Backfill seguro: las filas previas quedan con dedup_key = source_url (ya único),
-- así la unique constraint se puede crear sin choques. Las nuevas ingestas usan la
-- clave canónica sha256; source_url sigue protegiendo a las antiguas.
UPDATE public.news_item SET dedup_key = source_url WHERE dedup_key IS NULL;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_news_item_dedup_key') THEN
        ALTER TABLE public.news_item ADD CONSTRAINT uk_news_item_dedup_key UNIQUE (dedup_key);
    END IF;
END $$;

-- content_hash es señal de apoyo (detectar edición upstream), no clave: índice no único.
CREATE INDEX IF NOT EXISTS idx_news_item_content_hash ON public.news_item (content_hash);
