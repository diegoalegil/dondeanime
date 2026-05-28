package com.dondeanime.backend.anime.anilist;

import java.util.List;

public record AniListStudioConnection(
        List<AniListStudio> nodes
) {}
