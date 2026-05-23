package com.dondeanime.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Configuración compartida para clientes HTTP.
 *
 * Aquí se declara un único RestClient.Builder que el resto de
 * clientes (AniListClient, TmdbClient en el futuro, etc.) inyectan
 * y especializan con su propia baseUrl.
 *
 * Cuando queramos timeouts, interceptors de logging, métricas o
 * autenticación común para todos los clientes, se añaden aquí
 * una sola vez y los heredan todos.
 */
@Configuration
public class HttpClientConfig {

    @Bean
    RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}
