package com.dondeanime.backend.news;

import java.time.Instant;

/** Vista de listado de una noticia (oculta cuerpo completo, ids internos, etc.). */
public record NewsSummaryDto(
        String slug,
        String title,
        String summary,
        String imageUrl,
        String sourceName,
        Long animeId,
        String animeSlug,
        Instant publishedAt) {

    public static NewsSummaryDto from(NewsItem item, String animeSlug) {
        return new NewsSummaryDto(
                item.getSlug(),
                item.getTitle(),
                item.getSummary(),
                item.getImageUrl(),
                item.getSourceName(),
                item.getAnimeId(),
                animeSlug,
                item.getPublishedAt());
    }
}
