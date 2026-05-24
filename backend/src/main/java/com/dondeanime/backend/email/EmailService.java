package com.dondeanime.backend.email;

import java.util.List;

public interface EmailService {

    void sendConfirmationEmail(
            String email,
            String animeTitle,
            String countryName,
            String confirmUrl);

    void sendAlertEmail(
            String email,
            String animeTitle,
            String countryName,
            List<String> providerNames,
            String unsubscribeUrl,
            String eraseUrl);
}
