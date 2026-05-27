package com.dondeanime.backend.anime.anilist;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Nombre del personaje. full es el nombre romanizado completo.
 */
public record AniListCharacterName(
        String full,
        @JsonProperty("native") String nativeName
) {}
