package com.dondeanime.backend.news;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Tests del seeder: siembra cuando falta y reconcilia la URL cuando cambia. */
class NewsSourceSeederTest {

    private final NewsSourceRepository repository = mock(NewsSourceRepository.class);
    private final NewsSourceSeeder seeder = new NewsSourceSeeder(repository);

    private static final String ANN_URL = "https://www.animenewsnetwork.com/all/rss.xml?ann-edition=us";
    private static final String CRUNCHYROLL_URL = "https://feeds.feedburner.com/crunchyroll/rss/news";

    @Test
    void seedsBothSourcesWhenAbsent() {
        when(repository.findByName(anyString())).thenReturn(Optional.empty());

        seeder.run(null);

        ArgumentCaptor<NewsSource> captor = ArgumentCaptor.forClass(NewsSource.class);
        verify(repository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(NewsSource::getName)
                .containsExactlyInAnyOrder("Anime News Network", "Crunchyroll");
        assertThat(captor.getAllValues()).allMatch(s -> s.getType() == NewsSourceType.RSS);
    }

    @Test
    void reconcilesUrlWhenChangedInCode() {
        NewsSource annOld = new NewsSource("Anime News Network", NewsSourceType.RSS, "https://old.ann/rss");
        when(repository.findByName("Anime News Network")).thenReturn(Optional.of(annOld));
        when(repository.findByName("Crunchyroll")).thenReturn(Optional.of(
                new NewsSource("Crunchyroll", NewsSourceType.RSS, CRUNCHYROLL_URL)));

        seeder.run(null);

        // Solo ANN se re-guarda (su URL cambió); Crunchyroll está al día.
        ArgumentCaptor<NewsSource> captor = ArgumentCaptor.forClass(NewsSource.class);
        verify(repository, times(1)).save(captor.capture());
        NewsSource saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("Anime News Network");
        assertThat(saved.getUrl()).isNotEqualTo("https://old.ann/rss");
        assertThat(saved.getUrl()).contains("animenewsnetwork");
    }

    @Test
    void doesNothingWhenSourcesUnchanged() {
        when(repository.findByName("Anime News Network")).thenReturn(Optional.of(
                new NewsSource("Anime News Network", NewsSourceType.RSS, ANN_URL)));
        when(repository.findByName("Crunchyroll")).thenReturn(Optional.of(
                new NewsSource("Crunchyroll", NewsSourceType.RSS, CRUNCHYROLL_URL)));

        seeder.run(null);

        verify(repository, never()).save(any());
    }
}
