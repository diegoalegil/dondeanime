package com.dondeanime.backend.news;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Siembra las fuentes RSS por defecto al arrancar, si no existen ya.
 *
 * Se hace por código (no por migración Flyway) a propósito: en local el schema
 * lo lleva Hibernate con Flyway desactivado, así que una migración de datos no
 * correría en dev. Es idempotente (clave por nombre) y solo crea/actualiza filas;
 * quién dispara la ingesta es el job/endpoint, no este seeder.
 *
 * Gateado por {@code news.seed-sources.enabled} (on por defecto; los tests lo
 * apagan) para no ensuciar la BD de los {@code @SpringBootTest}.
 *
 * URLs verificadas el 29 may 2026. ANN redirige a la edición US; feedburner es
 * el feed público de Crunchyroll. El fetcher sigue redirecciones por si cambian.
 */
@ConditionalOnProperty(name = "news.seed-sources.enabled", havingValue = "true", matchIfMissing = true)
@Component
public class NewsSourceSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(NewsSourceSeeder.class);

    private final NewsSourceRepository repository;

    public NewsSourceSeeder(NewsSourceRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(ApplicationArguments args) {
        seed("Anime News Network", "https://www.animenewsnetwork.com/all/rss.xml?ann-edition=us");
        seed("Crunchyroll", "https://feeds.feedburner.com/crunchyroll/rss/news");
    }

    private void seed(String name, String url) {
        Optional<NewsSource> existing = repository.findByName(name);
        if (existing.isPresent()) {
            // Reconcilia: si cambiamos la URL en código (ANN/Crunchyroll mueven su
            // ruta RSS), prod la adopta sin un UPDATE manual.
            NewsSource source = existing.get();
            if (!url.equals(source.getUrl())) {
                source.setUrl(url);
                repository.save(source);
                log.info("[news] URL de fuente '{}' actualizada", name);
            }
            return;
        }
        repository.save(new NewsSource(name, NewsSourceType.RSS, url));
        log.info("[news] fuente RSS sembrada: {}", name);
    }
}
