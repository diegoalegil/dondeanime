package com.dondeanime.backend.subscription;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dondeanime.backend.anime.Anime;
import com.dondeanime.backend.email.EmailService;
import com.dondeanime.backend.provider.WatchProvider;

@Service
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    private final SubscriptionRepository subscriptionRepository;
    private final EmailTokenService emailTokenService;
    private final SubscriptionService subscriptionService;
    private final EmailService emailService;

    public AlertService(
            SubscriptionRepository subscriptionRepository,
            EmailTokenService emailTokenService,
            SubscriptionService subscriptionService,
            EmailService emailService) {
        this.subscriptionRepository = subscriptionRepository;
        this.emailTokenService = emailTokenService;
        this.subscriptionService = subscriptionService;
        this.emailService = emailService;
    }

    @Transactional
    public int notifyNewProviders(Anime anime, Map<String, List<WatchProvider>> providersByCountry) {
        if (providersByCountry == null || providersByCountry.isEmpty()) {
            return 0;
        }

        int sent = 0;
        for (Map.Entry<String, List<WatchProvider>> entry : providersByCountry.entrySet()) {
            String countryCode = CountryCatalog.normalizeCountry(entry.getKey());
            List<Subscription> pending = subscriptionRepository.findPendingAlerts(anime.getId(), countryCode);
            if (pending.isEmpty()) {
                continue;
            }

            List<String> providerNames = entry.getValue().stream()
                    .map(WatchProvider::getProviderName)
                    .distinct()
                    .sorted(Comparator.naturalOrder())
                    .toList();

            for (Subscription subscription : pending) {
                try {
                    AppUser user = subscription.getUser();
                    IssuedEmailToken token = emailTokenService.createUnsubscribeToken(
                            user,
                            anime,
                            countryCode);
                    emailService.sendAlertEmail(
                            user.getEmail(),
                            animeTitle(anime),
                            CountryCatalog.countryName(countryCode),
                            providerNames,
                            subscriptionService.unsubscribeUrl(token.rawToken()),
                            subscriptionService.eraseUrl(user.getEmail(), token.rawToken()));
                    subscription.setNotifiedAt(Instant.now());
                    subscriptionRepository.save(subscription);
                    sent++;
                } catch (Exception e) {
                    log.error("No se pudo enviar alerta anime={} country={}: {}",
                            anime.getSlug(), countryCode, e.getMessage());
                }
            }
        }

        return sent;
    }

    private static String animeTitle(Anime anime) {
        return anime.getTitleEnglish() != null && !anime.getTitleEnglish().isBlank()
                ? anime.getTitleEnglish()
                : anime.getTitleRomaji();
    }
}
