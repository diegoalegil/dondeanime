package com.dondeanime.backend.config;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Configuración compartida para clientes HTTP.
 *
 * CRÍTICO: el bean es {@code @Scope("prototype")}. RestClient.Builder es MUTABLE.
 * Si fuera singleton (lo que pasa con un @Bean normal), TODOS los clientes
 * (AniListClient, TmdbClient, ResendEmailService, TelegramAlertService,
 * CatalogScheduler...) recibirían EL MISMO builder, y al llamar
 * {@code .defaultHeader(...)} lo mutarían para todos los demás.
 *
 * En concreto TmdbClient y ResendEmailService añaden
 * {@code Authorization: Bearer <token>}. Ese header se filtraba al AniListClient
 * según el orden de construcción de beans, y AniList respondía
 * 400 "Invalid token" porque recibía un token que no es suyo (el de TMDb).
 * Con prototype, cada punto de inyección recibe su PROPIO builder y nadie
 * contamina a nadie. Es justo lo que hace el autoconfig de Spring Boot; al
 * declararlo a mano sin scope, lo habíamos roto.
 *
 * Timeouts globales: sin ellos, si AniList/TMDb/Resend/Stripe/Trakt cuelgan la
 * conexión, el thread se queda colgado indefinidamente y agota el pool.
 *
 * IMPORTANTE: se usa el cliente JDK (java.net.http.HttpClient) explícito.
 * NO usar SimpleClientHttpRequestFactory (HttpURLConnection): rompe el
 * POST GraphQL de AniList (responde 400 "Invalid token"). El cliente JDK
 * envía el body correctamente.
 */
@Configuration
public class HttpClientConfig {

    /**
     * User-Agent identificable. AniList está detrás de Cloudflare y bloquea
     * (error 1010 → se manifiesta como 400 "Invalid token") a los clientes
     * sin User-Agent de navegador, como el cliente HTTP de Java por defecto.
     * Con un UA propio identificable, Cloudflare deja pasar la petición.
     *
     * OJO: el JDK trata User-Agent como header RESTRINGIDO y lo ignora salvo
     * que el contenedor arranque con
     * -Djdk.httpclient.allowRestrictedHeaders=user-agent (ver docker-compose.prod.yml).
     */
    private static final String USER_AGENT = "DondeAnime/1.0 (+https://dondeanime.com)";

    @Bean
    @Scope("prototype")
    RestClient.Builder restClientBuilder() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(60));
        return RestClient.builder()
                .requestFactory(factory)
                .defaultHeader("User-Agent", USER_AGENT);
    }
}
