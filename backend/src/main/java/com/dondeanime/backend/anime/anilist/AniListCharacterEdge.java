package com.dondeanime.backend.anime.anilist;

/**
 * Arista de la connection characters. role suele ser MAIN o SUPPORTING.
 */
public record AniListCharacterEdge(
        String role,
        AniListCharacter node
) {}
