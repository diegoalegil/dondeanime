ALTER TABLE public.anime
    ADD COLUMN IF NOT EXISTS trailer_youtube_id character varying(32);
