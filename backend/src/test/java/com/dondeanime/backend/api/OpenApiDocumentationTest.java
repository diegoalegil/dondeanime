package com.dondeanime.backend.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.dondeanime.backend.admin.auth.AdminJwtService;
import com.dondeanime.backend.affiliate.AffiliateLinkService;
import com.dondeanime.backend.anime.AnimeRepository;
import com.dondeanime.backend.anime.AnimeController;
import com.dondeanime.backend.anime.AnimeOverrideService;
import com.dondeanime.backend.anime.GenreController;
import com.dondeanime.backend.anime.RecommendationService;
import com.dondeanime.backend.anime.SeasonController;
import com.dondeanime.backend.config.SecurityConfig;
import com.dondeanime.backend.curated.CuratedListController;
import com.dondeanime.backend.curated.CuratedListService;
import com.dondeanime.backend.news.NewsController;
import com.dondeanime.backend.news.NewsService;
import com.dondeanime.backend.premium.PremiumAccessService;
import com.dondeanime.backend.provider.ProviderController;
import com.dondeanime.backend.provider.WatchProviderRepository;
import com.dondeanime.backend.sitemap.SitemapController;
import com.dondeanime.backend.studio.StudioController;
import com.dondeanime.backend.studio.StudioRepository;

import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiOAuthProperties;
import org.springdoc.webmvc.core.configuration.MultipleOpenApiSupportConfiguration;
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration;
import org.springdoc.webmvc.ui.SwaggerConfig;

@WebMvcTest({
        AnimeController.class,
        ProviderController.class,
        GenreController.class,
        SeasonController.class,
        NewsController.class,
        CuratedListController.class,
        StudioController.class,
        SitemapController.class,
        OpenApiDocsController.class
})
@Import({SecurityConfig.class, OpenApiConfig.class, AdminJwtService.class})
@ImportAutoConfiguration({
        SpringDocConfiguration.class,
        SpringDocConfigProperties.class,
        SpringDocWebMvcConfiguration.class,
        MultipleOpenApiSupportConfiguration.class,
        SwaggerConfig.class,
        SwaggerUiConfigProperties.class,
        SwaggerUiOAuthProperties.class
})
@TestPropertySource(properties = {
        "admin.username=admin",
        "admin.password=secret",
        "admin.cors.allowed-origins=http://localhost:4321",
        "alerts.jwt-secret=test-jwt-secret",
        "springdoc.api-docs.enabled=true",
        "springdoc.swagger-ui.enabled=true"
})
class OpenApiDocumentationTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AnimeRepository animeRepository;

    @MockitoBean
    private WatchProviderRepository providerRepository;

    @MockitoBean
    private AnimeOverrideService overrideService;

    @MockitoBean
    private AffiliateLinkService affiliateLinkService;

    @MockitoBean
    private RecommendationService recommendationService;

    @MockitoBean
    private NewsService newsService;

    @MockitoBean
    private CuratedListService curatedListService;

    @MockitoBean
    private PremiumAccessService premiumAccessService;

    @MockitoBean
    private StudioRepository studioRepository;

    @Test
    void openApiYamlGeneratesPublicV1Spec() throws Exception {
        when(animeRepository.findAll()).thenReturn(List.of());
        when(providerRepository.aggregateAllProviders()).thenReturn(List.of());

        mvc.perform(get("/v3/api-docs.yaml/public-v1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("openapi:")))
                .andExpect(content().string(containsString("ApiKeyAuth")))
                .andExpect(content().string(containsString("/api/v1/anime")))
                .andExpect(content().string(containsString("/api/v1/providers")))
                .andExpect(content().string(containsString("/api/v1/genres")))
                .andExpect(content().string(containsString("/api/v1/seasons")))
                .andExpect(content().string(containsString("/api/v1/news")))
                .andExpect(content().string(containsString("/api/v1/lists")))
                .andExpect(content().string(containsString("/api/v1/studios")))
                .andExpect(content().string(containsString("/api/v1/sitemap")))
                .andExpect(content().string(not(containsString("/api/anime"))))
                .andExpect(content().string(not(containsString("/api/v1/anime/match"))))
                .andExpect(content().string(not(containsString("/api/v1/anime/sync"))))
                .andExpect(content().string(not(containsString("/api/admin"))));
    }

    @Test
    void docsEndpointRedirectsToSwaggerUi() throws Exception {
        mvc.perform(get("/api/v1/docs"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/swagger-ui/index.html?urls.primaryName=public-v1"));
    }
}
