package com.dondeanime.backend.anime.anilist;

/**
 * URL de la imagen de portada en su tamaño grande.
 * AniList ofrece más tamaños (medium, small, color), pero solo
 * pedimos "large" en la query para mantenerlo simple.
 */
public record AniListCoverImage(String large) {}
