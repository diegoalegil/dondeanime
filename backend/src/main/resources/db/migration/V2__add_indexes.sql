-- Índices operativos para endpoints públicos con más lectura.
-- idx_watch_provider_country ya existe en el baseline de producción;
-- se mantiene aquí como no-op defensivo para instalaciones limpias o antiguas.
CREATE INDEX IF NOT EXISTS idx_watch_provider_country
    ON public.watch_provider USING btree (country_code);

CREATE INDEX IF NOT EXISTS idx_watch_provider_anime_country
    ON public.watch_provider USING btree (anime_id, country_code);

CREATE INDEX IF NOT EXISTS idx_anime_season_year
    ON public.anime USING btree (season, season_year);

CREATE INDEX IF NOT EXISTS idx_anime_popularity_desc
    ON public.anime USING btree (popularity DESC);

CREATE INDEX IF NOT EXISTS idx_anime_genre_genre
    ON public.anime_genre USING btree (genre);
