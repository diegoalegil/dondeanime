ALTER TABLE public.user_watched_anime
    ADD COLUMN IF NOT EXISTS rating integer;

ALTER TABLE public.user_watched_anime
    ADD COLUMN IF NOT EXISTS rated_at timestamp(6) with time zone;

CREATE INDEX IF NOT EXISTS idx_user_watched_anime_rated_at
    ON public.user_watched_anime USING btree (rated_at);
