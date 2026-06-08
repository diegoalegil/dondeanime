package com.dondeanime.backend.anime;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

class AnimeRepositorySearchVectorTest {

    @Test
    void repositoryMethodUsesPostgresFullTextQuery() throws NoSuchMethodException {
        Query query = AnimeRepository.class
                .getMethod("findIdsBySearchVectorMatching", String.class, int.class)
                .getAnnotation(Query.class);

        assertThat(query).isNotNull();
        assertThat(query.nativeQuery()).isTrue();
        assertThat(query.value()).contains("SELECT id");
        assertThat(query.value()).contains("search_vector @@ plainto_tsquery('spanish', :query)");
        assertThat(query.value()).contains("ts_rank(search_vector, plainto_tsquery('spanish', :query))");
        assertThat(query.value()).contains("LIMIT :limit");
    }

    @Test
    void migrationCreatesGeneratedTsvectorAndGinIndex() throws Exception {
        Path migration = Path.of(
                "src", "main", "resources", "db", "migration", "V3__anime_search_vector.sql");

        String sql = Files.readString(migration);

        assertThat(sql).contains("ADD COLUMN IF NOT EXISTS search_vector tsvector");
        assertThat(sql).contains("GENERATED ALWAYS AS");
        assertThat(sql).contains("USING GIN (search_vector)");
    }
}
