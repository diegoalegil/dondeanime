ALTER TABLE public.anime
    ADD COLUMN IF NOT EXISTS episode_duration integer;

ALTER TABLE public.anime
    ADD COLUMN IF NOT EXISTS studio character varying(120);

CREATE INDEX IF NOT EXISTS idx_anime_episode_duration
    ON public.anime USING btree (episode_duration);

CREATE INDEX IF NOT EXISTS idx_anime_studio_slug
    ON public.anime USING btree (lower(replace(studio, ' ', '-')));
