package com.dondeanime.backend.trakt;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class ExternalAccountMigrationTest {

    @Test
    void migrationCreatesUniqueExternalAccountsAndWatchedRows() throws Exception {
        String sql = Files.readString(Path.of(
                "src",
                "main",
                "resources",
                "db",
                "migration",
                "V14__external_accounts_and_watched_anime.sql"));

        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS public.external_account");
        assertThat(sql).contains("access_token_ciphertext character varying(2048)");
        assertThat(sql).contains("refresh_token_ciphertext character varying(2048)");
        assertThat(sql).contains("CONSTRAINT uk_external_account_provider_user UNIQUE (provider, external_user_id)");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS public.user_watched_anime");
        assertThat(sql).contains("CONSTRAINT uk_user_watched_anime_account_slug_source UNIQUE (external_account_id, anime_slug, source)");
        assertThat(sql).contains("ON DELETE CASCADE");
    }
}
