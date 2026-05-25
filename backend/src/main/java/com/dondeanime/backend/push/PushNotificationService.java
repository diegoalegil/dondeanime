package com.dondeanime.backend.push;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.dondeanime.backend.anime.Anime;
import com.dondeanime.backend.provider.WatchProvider;
import com.dondeanime.backend.subscription.CountryCatalog;
import com.dondeanime.backend.subscription.SubscriptionRepository;

import tools.jackson.databind.ObjectMapper;

@Service
public class PushNotificationService {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationService.class);

    private final SubscriptionRepository subscriptionRepository;
    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final WebPushService webPushService;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public PushNotificationService(
            SubscriptionRepository subscriptionRepository,
            PushSubscriptionRepository pushSubscriptionRepository,
            WebPushService webPushService) {
        this(subscriptionRepository, pushSubscriptionRepository, webPushService, new ObjectMapper(), Clock.systemUTC());
    }

    PushNotificationService(
            SubscriptionRepository subscriptionRepository,
            PushSubscriptionRepository pushSubscriptionRepository,
            WebPushService webPushService,
            ObjectMapper objectMapper,
            Clock clock) {
        this.subscriptionRepository = subscriptionRepository;
        this.pushSubscriptionRepository = pushSubscriptionRepository;
        this.webPushService = webPushService;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public int notifyNewProviders(Anime anime, String countryCode, List<WatchProvider> providers) {
        String normalizedCountry = CountryCatalog.normalizeCountry(countryCode);
        Set<String> pendingEmails = subscriptionRepository.findPendingAlerts(anime.getId(), normalizedCountry).stream()
                .map(subscription -> subscription.getUser().getEmail())
                .filter(Objects::nonNull)
                .map(email -> email.trim().toLowerCase(Locale.ROOT))
                .filter(email -> !email.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (pendingEmails.isEmpty()) {
            return 0;
        }

        List<PushSubscription> pushSubscriptions = pushSubscriptionRepository
                .findByCountryIsoAndUserEmailInOrderByCreatedAtAsc(normalizedCountry, pendingEmails);
        if (pushSubscriptions.isEmpty()) {
            return 0;
        }

        String payload = payloadFor(anime, normalizedCountry, providers);
        int sent = 0;
        for (PushSubscription subscription : pushSubscriptions) {
            try {
                int status = webPushService.send(subscription, payload).orElse(0);
                if (status >= 200 && status < 300) {
                    recordDelivery(subscription, status);
                    sent++;
                } else if (status > 0) {
                    recordDelivery(subscription, status);
                    log.warn("Push no aceptado endpoint={} status={}", subscription.getEndpoint(), status);
                }
            } catch (Exception e) {
                recordDelivery(subscription, 0);
                log.error("No se pudo enviar push endpoint={}: {}", subscription.getEndpoint(), e.getMessage());
            }
        }
        return sent;
    }

    private void recordDelivery(PushSubscription subscription, int status) {
        subscription.recordDeliveryResult(status, Instant.now(clock));
        pushSubscriptionRepository.save(subscription);
    }

    private String payloadFor(Anime anime, String countryCode, List<WatchProvider> providers) {
        List<String> providerNames = providers.stream()
                .map(WatchProvider::getProviderName)
                .filter(Objects::nonNull)
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();
        String providerText = String.join(", ", providerNames);
        String title = animeTitle(anime) + " ya esta disponible";
        String body = providerText.isBlank()
                ? "Nueva plataforma disponible en " + countryCode + "."
                : "Nuevo en " + countryCode + ": " + providerText + ".";

        try {
            return objectMapper.writeValueAsString(new PushNotificationPayload(
                    title,
                    body,
                    "/anime/" + anime.getSlug(),
                    "provider-added-" + anime.getId() + "-" + countryCode));
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo construir el payload push", e);
        }
    }

    private static String animeTitle(Anime anime) {
        return anime.getTitleEnglish() != null && !anime.getTitleEnglish().isBlank()
                ? anime.getTitleEnglish()
                : anime.getTitleRomaji();
    }
}
