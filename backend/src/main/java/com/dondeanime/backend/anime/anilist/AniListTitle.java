package com.dondeanime.backend.anime.anilist;

/**
 * Títulos del anime. Cualquiera de los tres puede ser null.
 *
 * <p>{@code nativeTitle} mapea el campo GraphQL {@code native} (palabra reservada en Java),
 * por lo que la query usa el alias {@code nativeTitle: native}.
 */
public record AniListTitle(String romaji, String english, String nativeTitle) {}
