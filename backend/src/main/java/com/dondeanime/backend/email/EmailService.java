package com.dondeanime.backend.email;

import java.util.List;

public interface EmailService {

    void sendConfirmationEmail(
            String email,
            String animeTitle,
            String countryName,
            String confirmUrl);

    void sendNewsletterConfirmationEmail(
            String email,
            String confirmUrl);

    void sendAlertEmail(
            String email,
            String animeTitle,
            String countryName,
            List<String> providerNames,
            String unsubscribeUrl,
            String eraseUrl);

    void sendPremiumWelcomeEmail(
            String email,
            String planTier,
            String manageUrl);

    void sendPremiumReceiptEmail(
            String email,
            String planTier,
            String paidAt,
            String manageUrl);

    void sendPremiumCancellationEmail(
            String email,
            String planTier,
            String premiumUrl);
}
