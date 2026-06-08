package com.dondeanime.backend.news;

import java.time.Instant;

/**
 * Una entrada cruda leída de un feed RSS/Atom, antes de persistirla.
 * Todo en su idioma original (normalmente inglés); el paso editorial posterior
 * decide resumen, cuerpo público y publicación.
 */
public record FetchedNewsItem(
        String title,
        String url,
        String excerpt,
        String imageUrl,
        Instant publishedAt) {
}
