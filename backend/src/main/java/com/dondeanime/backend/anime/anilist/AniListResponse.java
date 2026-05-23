package com.dondeanime.backend.anime.anilist;

/**
 * Raíz de la respuesta de AniList. Espejo del nodo "data" del JSON.
 */
public record AniListResponse(AniListData data) {}
