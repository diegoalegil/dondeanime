package com.dondeanime.backend.anime.anilist;

import java.util.List;

/**
 * Connection GraphQL de personajes de un anime.
 */
public record AniListCharacterConnection(List<AniListCharacterEdge> edges) {}
