package com.dondeanime.backend.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Configuración compartida para clientes HTTP.
 *
 * Aquí se declara un único RestClient.Builder que el resto de
 * clientes (AniListClient, TmdbClient, etc.) inyectan
 * y especializan con su propia baseUrl.
 *
 * Timeouts globales: sin ellos, si AniList/TMDb/Resend/Stripe/Trakt
 * cuelgan la conexión, el thread (sync o request) se queda colgado
 * indefinidamente y agota el pool. 10s para conectar y 60s para leer
 * son holgados para APIs que responden en <2s normalmente, pero cortan
 * los cuelgues reales. Si algún cliente necesita otro valor, puede
 * especializar su propio RestClient sobre este builder.
 */
@Configuration
public class HttpClientConfig {

    @Bean
    RestClient.Builder restClientBuilder() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(60));
        return RestClient.builder().requestFactory(factory);
    }
}
