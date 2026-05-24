package com.dondeanime.backend.subscription;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

@WebMvcTest(SubscriptionController.class)
class SubscriptionControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private SubscriptionService subscriptionService;

    @Test
    void postSubscriptionIsPublicAndReturnsAccepted() throws Exception {
        mvc.perform(post("/api/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"diego@example.com","animeSlug":"attack-on-titan","country":"ES"}
                                """))
                .andExpect(status().isAccepted())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        verify(subscriptionService).requestSubscription(any(SubscriptionRequest.class));
    }

    @Test
    void postSubscriptionRejectsInvalidEmail() throws Exception {
        mvc.perform(post("/api/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"no-es-email","animeSlug":"attack-on-titan","country":"ES"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void confirmReturnsHtml() throws Exception {
        org.mockito.Mockito.when(subscriptionService.confirmSubscription("token"))
                .thenReturn(new ConfirmedSubscription(
                        "diego@example.com",
                        "Attack on Titan",
                        "España"));

        mvc.perform(get("/api/subscriptions/confirm").queryParam("token", "token"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Alerta confirmada")));
    }

    @Test
    void unsubscribeGetOnlyShowsConfirmationPage() throws Exception {
        mvc.perform(get("/api/subscriptions/unsubscribe").queryParam("token", "token"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Darme de baja")));

        verifyNoInteractions(subscriptionService);
    }

    @Test
    void unsubscribePostMarksUserAsUnsubscribed() throws Exception {
        org.mockito.Mockito.when(subscriptionService.unsubscribe("token"))
                .thenReturn(new ConfirmedSubscription(
                        "diego@example.com",
                        "Attack on Titan",
                        "España"));

        mvc.perform(post("/api/subscriptions/unsubscribe").queryParam("token", "token"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Baja completada")));

        verify(subscriptionService).unsubscribe("token");
    }
}
