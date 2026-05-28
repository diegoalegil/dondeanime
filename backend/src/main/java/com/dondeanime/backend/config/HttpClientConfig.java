package com.dondeanime.backend.config;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Configuración compartida para clientes HTTP.
 *
 * Aquí se declara un único RestClient.Builder que el resto de
 * clientes (AniListClient, TmdbClient, etc.) inyectan
 * y especializan con su propia baseUrl.
 *
 * Timeouts globales: sin ellos, si AniList/TMDb/Resend/Stripe/Trakt
 * cuelgan la conexión, el thread se queda colgado indefinidamente y
 * agota el pool.
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
     */
    private static final String USER_AGENT = "DondeAnime/1.0 (+https://dondeanime.com)";

    @Bean
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
