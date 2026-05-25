package com.dondeanime.backend.push;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.dondeanime.backend.subscription.SubscriptionRepository;

import tools.jackson.databind.ObjectMapper;

@Service
public class NotificationDashboardService {

    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final WebPushService webPushService;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public NotificationDashboardService(
            PushSubscriptionRepository pushSubscriptionRepository,
            SubscriptionRepository subscriptionRepository,
            WebPushService webPushService,
            ObjectMapper objectMapper) {
        this(pushSubscriptionRepository, subscriptionRepository, webPushService, objectMapper, Clock.systemUTC());
    }

    NotificationDashboardService(
            PushSubscriptionRepository pushSubscriptionRepository,
            SubscriptionRepository subscriptionRepository,
            WebPushService webPushService,
            ObjectMapper objectMapper,
            Clock clock) {
        this.pushSubscriptionRepository = pushSubscriptionRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.webPushService = webPushService;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public NotificationStatsDto stats() {
        Instant since = Instant.now(clock).minus(24, ChronoUnit.HOURS);
        List<PushSubscription> subscriptions = pushSubscriptionRepository.findAllByOrderByCreatedAtDesc();
        long successes = subscriptions.stream()
                .mapToLong(subscription -> count(subscription.getDeliverySuccessCount()))
                .sum();
        long failures = subscriptions.stream()
                .mapToLong(subscription -> count(subscription.getDeliveryFailureCount()))
                .sum();
        long attempts = successes + failures;
        double deliveryRate = attempts == 0 ? 0.0 : (successes * 100.0) / attempts;

        return new NotificationStatsDto(
                subscriptions.size(),
                subscriptionRepository.countByNotifiedAtAfter(since),
                attempts,
                successes,
                failures,
                deliveryRate,
                webPushService.isConfigured(),
                subscriptions.stream().map(PushSubscriptionAdminDto::from).toList());
    }

    @Transactional
    public NotificationTestResponse sendTestPush(Long subscriptionId) {
        PushSubscription subscription = pushSubscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subscription push no encontrada"));
        String payload = testPayload(subscription);
        try {
            Integer status = webPushService.send(subscription, payload).orElse(null);
            if (status == null) {
                return new NotificationTestResponse(false, null, "Web Push no configurado.");
            }
            subscription.recordDeliveryResult(status, Instant.now(clock));
            pushSubscriptionRepository.save(subscription);
            boolean sent = status >= 200 && status < 300;
            return new NotificationTestResponse(
                    sent,
                    status,
                    sent ? "Push test enviado." : "Push test rechazado.");
        } catch (Exception e) {
            subscription.recordDeliveryResult(0, Instant.now(clock));
            pushSubscriptionRepository.save(subscription);
            return new NotificationTestResponse(false, 0, "No se pudo enviar push test.");
        }
    }

    private String testPayload(PushSubscription subscription) {
        try {
            return objectMapper.writeValueAsString(new PushNotificationPayload(
                    "DondeAnime test push",
                    "La configuracion push funciona para " + subscription.getCountryIso() + ".",
                    "/admin/notifications",
                    "admin-test-push-" + subscription.getId() + "-" + Instant.now(clock).toEpochMilli()));
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo construir el payload push test", e);
        }
    }

    private static int count(Integer value) {
        return value == null ? 0 : value;
    }
}
