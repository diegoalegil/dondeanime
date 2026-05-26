package com.dondeanime.backend.newsletter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(NewsletterController.class)
class NewsletterControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private NewsletterService newsletterService;

    @Test
    void subscribeIsPublicAndReturnsAccepted() throws Exception {
        mvc.perform(post("/api/newsletter/subscribe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"diego@example.com","privacyAccepted":true}
                                """))
                .andExpect(status().isAccepted())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        verify(newsletterService).requestSubscription(any(NewsletterSubscribeRequest.class));
    }

    @Test
    void subscribeRejectsMissingPrivacyConsent() throws Exception {
        mvc.perform(post("/api/newsletter/subscribe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"diego@example.com","privacyAccepted":false}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void confirmReturnsHtml() throws Exception {
        org.mockito.Mockito.when(newsletterService.confirmSubscription("token"))
                .thenReturn("diego@example.com");

        mvc.perform(get("/api/newsletter/confirm").queryParam("token", "token"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Newsletter confirmada")));

        verify(newsletterService).confirmSubscription("token");
    }
}
