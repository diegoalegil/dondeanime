package com.dondeanime.backend.news;

import java.time.Instant;

/**
 * Una entrada cruda leída de un feed RSS/Atom, antes de persistirla.
 * Todo en su idioma original (normalmente inglés); la traducción/resumen al
 * español la hace el LLM en una fase posterior (S2).
 */
public record FetchedNewsItem(
        String title,
        String url,
        String excerpt,
        String imageUrl,
        Instant publishedAt) {
}
