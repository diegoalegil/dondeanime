package com.dondeanime.backend.news;

import java.time.Instant;

/** Vista de detalle de una noticia para la página pública. */
public record NewsDetailDto(
        String slug,
        String title,
        String summary,
        String body,
        String imageUrl,
        String sourceName,
        String sourceUrl,
        Long animeId,
        String metaTitle,
        String metaDescription,
        Instant publishedAt) {

    public static NewsDetailDto from(NewsItem item) {
        return new NewsDetailDto(
                item.getSlug(),
                item.getTitle(),
                item.getSummary(),
                item.getBody(),
                item.getImageUrl(),
                item.getSourceName(),
                item.getSourceUrl(),
                item.getAnimeId(),
                item.getMetaTitle(),
                item.getMetaDescription(),
                item.getPublishedAt());
    }
}
