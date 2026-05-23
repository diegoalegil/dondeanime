package com.dondeanime.backend.anime.anilist;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Envuelve la página. En el JSON se llama "Page" con P mayúscula,
 * por eso @JsonProperty mapea el nombre al campo "page" en camelCase.
 */
public record AniListData(@JsonProperty("Page") AniListPage page) {}
