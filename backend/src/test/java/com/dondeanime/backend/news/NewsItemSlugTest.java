package com.dondeanime.backend.news;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NewsItemSlugTest {

    @Test
    void slugifyNormalizaTildesYsimbolos() {
        assertThat(NewsItem.slugify("Ataque a los Titanes: ¡Final!"))
                .isEqualTo("ataque-a-los-titanes-final");
    }

    @Test
    void slugifyColapsaEspaciosYguiones() {
        assertThat(NewsItem.slugify("  Nueva   temporada  ")).isEqualTo("nueva-temporada");
    }

    @Test
    void slugifyVacioDevuelveVacio() {
        assertThat(NewsItem.slugify("")).isEqualTo("");
        assertThat(NewsItem.slugify(null)).isEqualTo("");
    }
}
