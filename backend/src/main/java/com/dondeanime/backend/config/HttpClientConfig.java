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

    @Bean
    RestClient.Builder restClientBuilder() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(60));
        return RestClient.builder().requestFactory(factory);
    }
}
