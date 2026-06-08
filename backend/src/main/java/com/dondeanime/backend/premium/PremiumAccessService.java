package com.dondeanime.backend.premium;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.dondeanime.backend.email.EmailService;

@Service
public class PremiumAccessService {

    private static final String BEARER_PREFIX = "Bearer ";

    private final SubscriberService subscriberService;
    private final PremiumAccessTokenService tokenService;
    private final EmailService emailService;
    private final String siteUrl;

    public PremiumAccessService(
            SubscriberService subscriberService,
            PremiumAccessTokenService tokenService,
            EmailService emailService,
            @Value("${dondeanime.site-url:https://dondeanime.com}") String siteUrl) {
        this.subscriberService = subscriberService;
        this.tokenService = tokenService;
        this.emailService = emailService;
        this.siteUrl = siteUrl;
    }

    public void requestAccessLink(String email) {
        String normalizedEmail = SubscriberService.normalizeEmail(email);
        if (normalizedEmail.isBlank()) {
            return;
        }
        subscriberService.findActiveEntitlement(normalizedEmail)
                .ifPresent(entitlement -> emailService.sendPremiumAccessEmail(
                        entitlement.email(),
                        entitlement.planTier(),
                        accessUrl(tokenService.createToken(entitlement))));
    }

    public PremiumStatusResponse status(String authorization) {
        return entitlementFromAuthorization(authorization)
                .map(PremiumStatusResponse::active)
                .orElseGet(PremiumStatusResponse::inactive);
    }

    public boolean hasActivePremiumAccess(String authorization) {
        return entitlementFromAuthorization(authorization).isPresent();
    }

    private Optional<PremiumEntitlement> entitlementFromAuthorization(String authorization) {
        String token = bearerToken(authorization);
        if (token.isBlank()) {
            return Optional.empty();
        }
        return tokenService.resolveEmail(token)
                .flatMap(subscriberService::findActiveEntitlement);
    }

    private String accessUrl(String token) {
        return siteUrl.replaceAll("/+$", "")
                + "/premium?access_token="
                + URLEncoder.encode(token, StandardCharsets.UTF_8);
    }

    private static String bearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return "";
        }
        return authorization.substring(BEARER_PREFIX.length()).trim();
    }
}
