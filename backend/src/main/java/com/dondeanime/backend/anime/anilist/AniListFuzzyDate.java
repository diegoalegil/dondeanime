package com.dondeanime.backend.anime.anilist;

/**
 * Fecha "difusa" de AniList. Cualquiera de los tres campos puede ser null
 * (un anime puede tener solo año, año+mes, o nada confirmado).
 */
public record AniListFuzzyDate(Integer year, Integer month, Integer day) {}
