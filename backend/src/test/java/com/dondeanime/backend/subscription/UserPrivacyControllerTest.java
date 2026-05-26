package com.dondeanime.backend.subscription;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserPrivacyController.class)
class UserPrivacyControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private SubscriptionService subscriptionService;

    @Test
    void eraseDeletesUserData() throws Exception {
        mvc.perform(delete("/api/users/diego@example.com/erase").queryParam("token", "raw.jwt"))
                .andExpect(status().isNoContent());

        verify(subscriptionService).eraseUser("diego@example.com", "raw.jwt");
    }

    @Test
    void erasePageReturnsHtmlWithEscapedData() throws Exception {
        mvc.perform(get("/api/users/diego@example.com/erase").queryParam("token", "raw.jwt"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(Matchers.containsString("Borrar datos")))
                .andExpect(content().string(Matchers.containsString("diego@example.com")))
                .andExpect(content().string(Matchers.containsString("diego%40example.com")));
    }
}
