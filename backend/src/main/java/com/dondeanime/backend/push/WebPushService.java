package com.dondeanime.backend.push;

import java.security.GeneralSecurityException;
import java.security.Security;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dondeanime.backend.subscription.CountryCatalog;

import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;

@Service
public class WebPushService {

    private static final Logger log = LoggerFactory.getLogger(WebPushService.class);

    private final PushSubscriptionRepository repository;
    private final String vapidPublicKey;
    private final String vapidPrivateKey;
    private final String vapidSubject;

    public WebPushService(
            PushSubscriptionRepository repository,
            @Value("${VAPID_PUBLIC_KEY:}") String vapidPublicKey,
            @Value("${VAPID_PRIVATE_KEY:}") String vapidPrivateKey,
            @Value("${VAPID_SUBJECT:mailto:contacto@dondeanime.com}") String vapidSubject) {
        this.repository = repository;
        this.vapidPublicKey = vapidPublicKey;
        this.vapidPrivateKey = vapidPrivateKey;
        this.vapidSubject = vapidSubject;
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    @Transactional
    public PushSubscription saveSubscription(PushSubscriptionRequest request) {
        String countryIso = CountryCatalog.normalizeCountry(request.countryIso());
        String userEmail = request.userEmail().trim().toLowerCase(Locale.ROOT);
        PushSubscription subscription = repository.findByEndpoint(request.endpoint())
                .orElseGet(PushSubscription::new);

        if (subscription.getCreatedAt() == null) {
            subscription.setCreatedAt(Instant.now());
        }
        subscription.setUserEmail(userEmail);
        subscription.setEndpoint(request.endpoint());
        subscription.setP256dh(request.keys().p256dh());
        subscription.setAuth(request.keys().auth());
        subscription.setCountryIso(countryIso);
        return repository.save(subscription);
    }

    public boolean isConfigured() {
        return !vapidPublicKey.isBlank() && !vapidPrivateKey.isBlank();
    }

    public Optional<Integer> send(PushSubscription subscription, String payload) {
        if (!isConfigured()) {
            log.debug("Web Push no configurado; se omite endpoint={}", subscription.getEndpoint());
            return Optional.empty();
        }
        try {
            PushService pushService = new PushService(vapidPublicKey, vapidPrivateKey, vapidSubject);
            Subscription webPushSubscription = new Subscription(
                    subscription.getEndpoint(),
                    new Subscription.Keys(subscription.getP256dh(), subscription.getAuth()));
            Notification notification = new Notification(webPushSubscription, payload);
            Object response = pushService.getClass()
                    .getMethod("send", Notification.class)
                    .invoke(pushService, notification);
            return Optional.of(readStatusCode(response));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo preparar la notificacion push", e);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("No se pudo leer la respuesta push", e);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo enviar la notificacion push", e);
        }
    }

    private static int readStatusCode(Object response) throws ReflectiveOperationException {
        Object statusLine = response.getClass().getMethod("getStatusLine").invoke(response);
        Object statusCode = statusLine.getClass().getMethod("getStatusCode").invoke(statusLine);
        if (statusCode instanceof Integer code) {
            return code;
        }
        throw new IllegalStateException("Respuesta push sin status code entero");
    }
}
