package com.dondeanime.backend.news;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import com.dondeanime.backend.anime.Anime;
import com.dondeanime.backend.anime.AnimeRepository;

class NewsProcessingServiceTest {

    private final NewsItemRepository itemRepository = mock(NewsItemRepository.class);
    private final AnimeRepository animeRepository = mock(AnimeRepository.class);
    // LLM apagado por defecto: la ruta heurística debe comportarse exactamente
    // igual que antes de integrar el LLM (los tests de abajo son la prueba).
    private final LlmNewsProcessor llmNewsProcessor = mock(LlmNewsProcessor.class);

    @Test
    void disabledProcessingDoesNotTouchRepositories() {
        NewsProcessingService service = service(false, false);

        NewsProcessingResult result = service.processDrafts();

        assertThat(result.enabled()).isFalse();
        assertThat(result.itemsProcessed()).isZero();
        verify(itemRepository, never()).findByStatusOrderByFetchedAtAsc(any(), any());
        verify(animeRepository, never()).findAllWithSynonyms();
    }

    @Test
    void enrichesDraftAndKeepsItUnpublishedByDefault() {
        NewsItem item = draft("Solo Leveling season 2 announced",
                "The Solo Leveling anime returns in January with a new trailer.");
        Anime anime = anime(7L, "solo-leveling", "Solo Leveling");
        when(itemRepository.findByStatusOrderByFetchedAtAsc(eq(NewsStatus.DRAFT), any(Pageable.class)))
                .thenReturn(List.of(item));
        when(animeRepository.findAllWithSynonyms()).thenReturn(List.of(anime));

        NewsProcessingResult result = service(true, false).processDrafts();

        assertThat(result.enabled()).isTrue();
        assertThat(result.draftsSeen()).isEqualTo(1);
        assertThat(result.itemsProcessed()).isEqualTo(1);
        assertThat(result.itemsPublished()).isZero();
        assertThat(result.animeMatched()).isEqualTo(1);
        assertThat(item.getStatus()).isEqualTo(NewsStatus.DRAFT);
        assertThat(item.getAnimeId()).isEqualTo(7L);
        assertThat(item.getSummary()).isEqualTo("The Solo Leveling anime returns in January with a new trailer.");
        assertThat(item.getBody()).isEqualTo("<p>The Solo Leveling anime returns in January with a new trailer.</p>");
        assertThat(item.getMetaTitle()).isEqualTo("Solo Leveling season 2 announced");
        assertThat(item.getMetaDescription()).contains("Solo Leveling anime returns");
        verify(itemRepository).save(item);
    }

    @Test
    void publishesOnlyWhenPublishFlagIsEnabled() {
        Instant fetchedAt = Instant.parse("2026-06-01T12:00:00Z");
        NewsItem item = draft("Frieren movie announced", "Frieren gets a new movie.");
        item.setFetchedAt(fetchedAt);
        when(itemRepository.findByStatusOrderByFetchedAtAsc(eq(NewsStatus.DRAFT), any(Pageable.class)))
                .thenReturn(List.of(item));
        when(animeRepository.findAllWithSynonyms()).thenReturn(List.of(anime(9L, "frieren", "Frieren")));

        NewsProcessingResult result = service(true, true).processDrafts();

        assertThat(result.itemsPublished()).isEqualTo(1);
        assertThat(item.getStatus()).isEqualTo(NewsStatus.PUBLISHED);
        assertThat(item.getPublishedAt()).isEqualTo(fetchedAt);
        assertThat(item.getAnimeId()).isEqualTo(9L);
        verify(itemRepository).save(item);
    }

    @Test
    void llmActiveWritesSpanishVersionInsteadOfHeuristic() {
        NewsItem item = draft("Solo Leveling season 2 announced",
                "The Solo Leveling anime returns in January.");
        when(llmNewsProcessor.enabled()).thenReturn(true);
        when(llmNewsProcessor.enrich(item)).thenAnswer(invocation -> {
            item.setTitle("Solo Leveling anuncia segunda temporada");
            item.setSummary("El anime de Solo Leveling vuelve en enero.");
            item.setBody("<p>El anime de Solo Leveling vuelve en enero.</p>");
            item.setMetaTitle("Solo Leveling anuncia segunda temporada");
            item.setMetaDescription("El anime de Solo Leveling vuelve en enero.");
            item.setLlmTokensUsed(920);
            return true;
        });
        when(itemRepository.findByStatusOrderByFetchedAtAsc(eq(NewsStatus.DRAFT), any(Pageable.class)))
                .thenReturn(List.of(item));
        when(animeRepository.findAllWithSynonyms())
                .thenReturn(List.of(anime(7L, "solo-leveling", "Solo Leveling")));

        NewsProcessingResult result = service(true, false).processDrafts();

        assertThat(result.llmProcessed()).isEqualTo(1);
        assertThat(result.llmFailed()).isZero();
        assertThat(result.itemsProcessed()).isEqualTo(1);
        assertThat(item.getTitle()).isEqualTo("Solo Leveling anuncia segunda temporada");
        assertThat(item.getAnimeId()).isEqualTo(7L);
        assertThat(item.getStatus()).isEqualTo(NewsStatus.DRAFT);
        verify(itemRepository).save(item);
    }

    @Test
    void llmFailureLeavesDraftUntouchedWithoutHeuristicFallback() {
        NewsItem item = draft("Frieren movie announced", "Frieren gets a new movie.");
        when(llmNewsProcessor.enabled()).thenReturn(true);
        when(llmNewsProcessor.enrich(item)).thenReturn(false);
        when(itemRepository.findByStatusOrderByFetchedAtAsc(eq(NewsStatus.DRAFT), any(Pageable.class)))
                .thenReturn(List.of(item));
        when(animeRepository.findAllWithSynonyms()).thenReturn(List.of());

        NewsProcessingResult result = service(true, true).processDrafts();

        assertThat(result.llmFailed()).isEqualTo(1);
        assertThat(result.itemsSkipped()).isEqualTo(1);
        assertThat(result.itemsPublished()).isZero();
        assertThat(item.getStatus()).isEqualTo(NewsStatus.DRAFT);
        assertThat(item.getSummary()).isNull();
        verify(itemRepository, never()).save(any());
    }

    @Test
    void llmDailyQuotaExhaustedSkipsWithoutCallingLlm() {
        NewsItem item = draft("Chainsaw Man part 2", "New arc confirmed.");
        when(llmNewsProcessor.enabled()).thenReturn(true);
        when(itemRepository.countByLlmTokensUsedIsNotNullAndUpdatedAtGreaterThanEqual(any(Instant.class)))
                .thenReturn(50L);
        when(itemRepository.findByStatusOrderByFetchedAtAsc(eq(NewsStatus.DRAFT), any(Pageable.class)))
                .thenReturn(List.of(item));
        when(animeRepository.findAllWithSynonyms()).thenReturn(List.of());

        NewsProcessingResult result = service(true, false).processDrafts();

        assertThat(result.itemsSkipped()).isEqualTo(1);
        assertThat(result.llmProcessed()).isZero();
        verify(llmNewsProcessor, never()).enrich(any());
        verify(itemRepository, never()).save(any());
    }

    @Test
    void alreadyEnrichedItemIsNotSentToLlmAgain() {
        NewsItem item = draft("Frieren movie announced", "Frieren gets a new movie.");
        item.setSummary("Frieren tendra pelicula.");
        item.setBody("<p>Frieren tendra pelicula.</p>");
        item.setLlmTokensUsed(800);
        when(llmNewsProcessor.enabled()).thenReturn(true);
        when(itemRepository.findByStatusOrderByFetchedAtAsc(eq(NewsStatus.DRAFT), any(Pageable.class)))
                .thenReturn(List.of(item));
        when(animeRepository.findAllWithSynonyms()).thenReturn(List.of());

        NewsProcessingResult result = service(true, true).processDrafts();

        verify(llmNewsProcessor, never()).enrich(any());
        assertThat(result.llmProcessed()).isZero();
        assertThat(result.itemsPublished()).isEqualTo(1);
        assertThat(item.getStatus()).isEqualTo(NewsStatus.PUBLISHED);
        verify(itemRepository).save(item);
    }

    private NewsProcessingService service(boolean enabled, boolean publish) {
        return new NewsProcessingService(
                itemRepository, animeRepository, llmNewsProcessor, enabled, publish, 20, 50);
    }

    private static NewsItem draft(String title, String excerpt) {
        NewsItem item = new NewsItem();
        item.setSlug(NewsItem.slugify(title));
        item.setTitle(title);
        item.setOriginalTitle(title);
        item.setOriginalExcerpt(excerpt);
        item.setSourceUrl("https://news.example/" + item.getSlug());
        item.setSourceName("ANN");
        item.setStatus(NewsStatus.DRAFT);
        return item;
    }

    private static Anime anime(Long id, String slug, String title) {
        Anime anime = new Anime();
        anime.setId(id);
        anime.setSlug(slug);
        anime.setTitleEnglish(title);
        anime.setTitleRomaji(title);
        anime.setSynonyms(Set.of(title));
        return anime;
    }
}
