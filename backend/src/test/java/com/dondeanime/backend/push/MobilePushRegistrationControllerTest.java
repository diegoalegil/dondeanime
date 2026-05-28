package com.dondeanime.backend.push;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MobilePushRegistrationController.class)
class MobilePushRegistrationControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private MobilePushRegistrationService registrationService;

    @Test
    void registersDeviceWithoutReturningToken() throws Exception {
        when(registrationService.register(any(MobilePushRegistrationRequest.class)))
                .thenReturn(new MobilePushRegistrationResponse(
                        "IOS",
                        "ES",
                        true,
                        "Dispositivo movil registrado para alertas solicitadas."));

        mvc.perform(post("/api/mobile/push/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "platform": "ios",
                                  "deviceToken": "secret-native-token",
                                  "countryIso": "es"
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.platform").value("IOS"))
                .andExpect(jsonPath("$.countryIso").value("ES"))
                .andExpect(jsonPath("$.alertsOnly").value(true))
                .andExpect(jsonPath("$.deviceToken").doesNotExist());
    }
}
