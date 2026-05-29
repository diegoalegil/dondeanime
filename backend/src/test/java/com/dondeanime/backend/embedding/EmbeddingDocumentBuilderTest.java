package com.dondeanime.backend.embedding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.dondeanime.backend.anime.Anime;
import com.dondeanime.backend.anime.AnimeRepository;
import com.dondeanime.backend.provider.WatchProvider;
import com.dondeanime.backend.provider.WatchProviderRepository;

import tools.jackson.databind.ObjectMapper;

class EmbeddingDocumentBuilderTest {

    @Test
    void buildsDeterministicDocumentsWithPublicFieldsOnly() throws Exception {
        Anime anime = anime();
        WatchProvider crunchyroll = provider(1L, "ES", "Crunchyroll");
        WatchProvider netflix = provider(1L, "ES", "Netflix");
        AnimeRepository animeRepository = mock(AnimeRepository.class);
        WatchProviderRepository providerRepository = mock(WatchProviderRepository.class);
        when(animeRepository.findAllWithGenres()).thenReturn(List.of(anime));
        when(providerRepository.findByAnimeIdInOrderByAnimeIdAscCountryCodeAscProviderTypeAscProviderNameAsc(List.of(1L)))
                .thenReturn(List.of(crunchyroll, netflix));

        List<AnimeSearchDocument> documents = new EmbeddingDocumentBuilder(
                animeRepository,
                providerRepository,
                "https://dondeanime.com/").buildDocuments();

        assertThat(documents).hasSize(1);
        AnimeSearchDocument document = documents.getFirst();
        assertThat(document.slug()).isEqualTo("attack-on-titan");
        assertThat(document.title()).isEqualTo("Attack on Titan");
        assertThat(document.spanishSynopsis()).isEqualTo("Humanidad contra titanes & misterio.");
        assertThat(document.genres()).containsExactly("Action", "Drama");
        assertThat(document.season()).isEqualTo("SPRING");
        assertThat(document.seasonYear()).isEqualTo(2013);
        assertThat(document.averageScore()).isEqualTo(85);
        assertThat(document.canonicalUrl()).isEqualTo("https://dondeanime.com/anime/attack-on-titan");
        assertThat(document.availability()).hasSize(1);
        assertThat(document.availability().getFirst().countryCode()).isEqualTo("ES");
        assertThat(document.availability().getFirst().platforms()).containsExactly("Crunchyroll", "Netflix");

        String json = new ObjectMapper().writeValueAsString(document);
        assertThat(json)
                .doesNotContain("\"id\"")
                .doesNotContain("\"tmdbId\"")
                .doesNotContain("\"syncedAt\"");
    }

    @Test
    void emptyCatalogDoesNotQueryProviders() {
        AnimeRepository animeRepository = mock(AnimeRepository.class);
        WatchProviderRepository providerRepository = mock(WatchProviderRepository.class);
        when(animeRepository.findAllWithGenres()).thenReturn(List.of());

        List<AnimeSearchDocument> documents = new EmbeddingDocumentBuilder(
                animeRepository,
                providerRepository,
                "https://dondeanime.com").buildDocuments();

        assertThat(documents).isEmpty();
        verify(animeRepository).findAllWithGenres();
    }

    private static Anime anime() {
        Anime anime = new Anime();
        anime.setId(1L);
        anime.setAnilistId(16498L);
        anime.setTmdbId(1429L);
        anime.setSyncedAt(Instant.parse("2026-05-27T10:00:00Z"));
        anime.setSlug("attack-on-titan");
        anime.setTitleEnglish("Attack on Titan");
        anime.setTitleRomaji("Shingeki no Kyojin");
        anime.setDescriptionEs("<p>Humanidad contra titanes &amp; misterio.</p>");
        anime.setSeason("SPRING");
        anime.setSeasonYear(2013);
        anime.setAverageScore(85);
        anime.setGenres(new HashSet<>(List.of("Drama", "Action")));
        return anime;
    }

    private static WatchProvider provider(Long animeId, String countryCode, String providerName) {
        WatchProvider provider = new WatchProvider();
        provider.setAnimeId(animeId);
        provider.setCountryCode(countryCode);
        provider.setProviderName(providerName);
        provider.setProviderType("FLATRATE");
        return provider;
    }
}
