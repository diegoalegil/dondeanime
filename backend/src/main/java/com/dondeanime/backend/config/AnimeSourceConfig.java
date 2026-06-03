package com.dondeanime.backend.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.diegoalegil.tsunagi.anilist.AniListClient;
import io.github.diegoalegil.tsunagi.http.RetryPolicy;
import io.github.diegoalegil.tsunagi.ratelimit.TokenBucketRateLimiter;
import io.github.diegoalegil.tsunagi.tmdb.TmdbClient;

/**
 * Cablea los clientes de la librería Tsunagi (AniList, TMDb) como beans, en
 * sustitución de los clientes propios que vivían en {@code anime.anilist} y
 * {@code anime.tmdb}. Aquí solo va la INFRAESTRUCTURA (timeout, User-Agent,
 * retry y rate limit); la POLÍTICA (idiomas, países, heurísticas de match)
 * sigue en los servicios de {@code anime}/{@code provider}.
 *
 * <p>El User-Agent es necesario para AniList: está tras Cloudflare, que bloquea
 * (error 1010 → 400 "Invalid token") a los clientes sin UA. java.net.http trata
 * {@code User-Agent} como header restringido, así que el contenedor debe arrancar
 * con {@code -Djdk.httpclient.allowRestrictedHeaders=user-agent}
 * (ver {@code docker-compose.prod.yml}).
 */
@Configuration
public class AnimeSourceConfig {

    /** Mismo literal que {@code config/HttpClientConfig} (que sirve al resto de clientes HTTP). */
    private static final String USER_AGENT = "DondeAnime/1.0 (+https://dondeanime.com)";

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);

    @Bean
    AniListClient aniListClient() {
        // Sin rate limiter: el sync pagina en bloques de <=50 y AniList lo tolera.
        RetryPolicy retry = RetryPolicy.exponentialBackoff(3, Duration.ofMillis(500));
        return new AniListClient(REQUEST_TIMEOUT, USER_AGENT, retry, null);
    }

    @Bean
    TmdbClient tmdbClient(@Value("${tmdb.api-key}") String apiKey) {
        // Un único rate limiter compartido por todos los usos del bean (~3 req/s),
        // que reemplaza los sleep(300ms) dispersos por los servicios.
        TokenBucketRateLimiter rateLimiter = new TokenBucketRateLimiter(3, Duration.ofSeconds(1));
        RetryPolicy retry = RetryPolicy.exponentialBackoff(3, Duration.ofMillis(500));
        return new TmdbClient(apiKey, REQUEST_TIMEOUT, USER_AGENT, retry, rateLimiter);
    }
}
