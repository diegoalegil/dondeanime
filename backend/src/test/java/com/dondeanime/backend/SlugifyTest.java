package com.dondeanime.backend;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.dondeanime.backend.anime.GenreSummaryDto;
import com.dondeanime.backend.provider.ProviderSummaryDto;

/**
 * Tests puros (sin Spring) de la normalización a slug que usan los
 * DTOs de salida. Si esta regla cambia se rompen los URLs públicos.
 */
class SlugifyTest {

    @Test
    void providerSlugLowercaseSingleWord() {
        assertThat(ProviderSummaryDto.slugify("Crunchyroll")).isEqualTo("crunchyroll");
        assertThat(ProviderSummaryDto.slugify("Netflix")).isEqualTo("netflix");
    }

    @Test
    void providerSlugReplacesSpacesWithDashes() {
        assertThat(ProviderSummaryDto.slugify("Amazon Prime Video")).isEqualTo("amazon-prime-video");
        assertThat(ProviderSummaryDto.slugify("HBO Max")).isEqualTo("hbo-max");
    }

    @Test
    void providerSlugCompoundWithMultipleSpaces() {
        assertThat(ProviderSummaryDto.slugify("Netflix Standard with Ads"))
                .isEqualTo("netflix-standard-with-ads");
    }

    @Test
    void genreSlugFollowsSameConvention() {
        assertThat(GenreSummaryDto.slugify("Action")).isEqualTo("action");
        assertThat(GenreSummaryDto.slugify("Slice of Life")).isEqualTo("slice-of-life");
        assertThat(GenreSummaryDto.slugify("Sci-Fi")).isEqualTo("sci-fi");
    }
}
