package com.dondeanime.backend.anime.anilist;

import java.util.List;

/**
 * Página de resultados. Contiene la lista de animes devuelta por la query.
 */
public record AniListPage(List<AniListMedia> media) {}
