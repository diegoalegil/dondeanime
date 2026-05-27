package com.dondeanime.backend.anime.anilist;

/**
 * Personaje devuelto por AniList dentro de media.characters.
 */
public record AniListCharacter(
        Long id,
        AniListCharacterName name,
        AniListCharacterImage image
) {}
