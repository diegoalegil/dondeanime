package com.dondeanime.backend.subscription;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AlertController.class)
class AlertControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private SubscriptionService subscriptionService;

    @Test
    void postAlertAliasIsPublicAndReturnsAccepted() throws Exception {
        mvc.perform(post("/api/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"diego@example.com","animeSlug":"attack-on-titan","country":"ES"}
                                """))
                .andExpect(status().isAccepted())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        verify(subscriptionService).requestSubscription(any(SubscriptionRequest.class));
    }

    @Test
    void postAlertAliasRejectsInvalidEmail() throws Exception {
        mvc.perform(post("/api/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"no-es-email","animeSlug":"attack-on-titan","country":"ES"}
                                """))
                .andExpect(status().isBadRequest());
    }
}
