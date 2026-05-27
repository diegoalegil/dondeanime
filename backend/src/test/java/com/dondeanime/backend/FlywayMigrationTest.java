package com.dondeanime.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

@ActiveProfiles("prod")
@SpringBootTest(properties = {
        "tmdb.api-key=test",
        "admin.username=admin",
        "admin.password=test-admin-password",
        "alerts.jwt-secret=test-jwt-secret",
        "resend.enabled=false",
        "resend.api-key=test-resend-key",
        "plausible.enabled=false",
        "scheduling.enabled=false"
})
class FlywayMigrationTest {

    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("dondeanime")
            .withUsername("dondeanime_user")
            .withPassword("test");

    static {
        POSTGRES.start();
    }

    @Autowired
    private JdbcTemplate jdbc;

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @AfterAll
    static void stopPostgres() {
        POSTGRES.stop();
    }

    @Test
    void prodContextMigratesAndValidatesCurrentSchema() {
        List<String> tables = jdbc.queryForList("""
                select table_name
                from information_schema.tables
                where table_schema = 'public'
                  and table_type = 'BASE TABLE'
                order by table_name
                """, String.class);

        assertThat(tables).contains(
                "affiliate_click_event",
                "affiliate_link",
                "admin_user",
                "anime",
                "anime_character",
                "anime_character_role",
                "anime_tag",
                "anime_genre",
                "anime_studio",
                "anime_override",
                "app_user",
                "api_key",
                "api_key_endpoint_usage",
                "availability_change_event",
                "email_token",
                "newsletter_subscriber",
                "newsletter_token",
                "push_subscription",
                "recommendation_event",
                "studio",
                "subscriber",
                "subscription",
                "watch_provider");

        List<String> indexes = jdbc.queryForList("""
                select indexname
                from pg_indexes
                where schemaname = 'public'
                order by indexname
                """, String.class);

        assertThat(indexes).contains(
                "idx_watch_provider_country",
                "idx_watch_provider_anime_country",
                "idx_anime_season_year",
                "idx_anime_popularity_desc",
                "idx_anime_genre_genre",
                "idx_anime_episode_duration",
                "idx_anime_studio_slug",
                "idx_anime_search_vector",
                "idx_push_subscription_country",
                "idx_push_subscription_email",
                "idx_subscriber_expires_at",
                "idx_api_key_owner_email",
                "idx_api_key_tier",
                "idx_api_key_endpoint_usage_api_key",
                "idx_api_key_endpoint_usage_endpoint");

        Integer successfulMigrations = jdbc.queryForObject(
                "select count(*) from flyway_schema_history where success = true",
                Integer.class);

        assertThat(successfulMigrations).isEqualTo(12);
    }
}
