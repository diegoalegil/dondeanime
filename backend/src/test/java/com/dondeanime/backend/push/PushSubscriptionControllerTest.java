package com.dondeanime.backend.push;

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

@WebMvcTest(PushSubscriptionController.class)
class PushSubscriptionControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private WebPushService webPushService;

    @Test
    void subscribeIsPublicAndReturnsAccepted() throws Exception {
        mvc.perform(post("/api/push/subscribe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userEmail":"diego@example.com",
                                  "endpoint":"https://push.example/123",
                                  "countryIso":"ES",
                                  "keys":{"p256dh":"public-key","auth":"auth-secret"}
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        verify(webPushService).saveSubscription(any(PushSubscriptionRequest.class));
    }

    @Test
    void subscribeRejectsInvalidEmail() throws Exception {
        mvc.perform(post("/api/push/subscribe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userEmail":"no-es-email",
                                  "endpoint":"https://push.example/123",
                                  "countryIso":"ES",
                                  "keys":{"p256dh":"public-key","auth":"auth-secret"}
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}
