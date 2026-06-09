package com.dondeanime.backend.api;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

@Configuration
public class OpenApiConfig {

    private static final String API_KEY_SCHEME = "ApiKeyAuth";

    @Bean
    OpenAPI dondeAnimeOpenApi() {
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes(API_KEY_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name(ApiKeyAuthenticationFilter.API_KEY_HEADER)))
                .addSecurityItem(new SecurityRequirement().addList(API_KEY_SCHEME))
                .info(new Info()
                        .title("DondeAnime Public API")
                        .version("v1")
                        .description("Catalogo publico de anime, plataformas, generos, temporadas, noticias, listas, estudios y sitemap.")
                        .contact(new Contact()
                                .name("DondeAnime")
                                .url("https://dondeanime.com"))
                        .license(new License()
                                .name("Proprietary")));
    }

    @Bean
    GroupedOpenApi publicV1OpenApi() {
        return GroupedOpenApi.builder()
                .group("public-v1")
                .pathsToMatch("/api/v1/**")
                .pathsToExclude("/api/v1/docs")
                .build();
    }
}
