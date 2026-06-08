package com.dondeanime.backend.email;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class ResendEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(ResendEmailService.class);

    private final RestClient restClient;
    private final String from;
    private final boolean enabled;

    public ResendEmailService(
            RestClient.Builder restClientBuilder,
            @Value("${resend.api-base}") String apiBase,
            @Value("${resend.api-key:}") String apiKey,
            @Value("${resend.from}") String from,
            @Value("${resend.enabled:false}") boolean enabled) {
        this.restClient = restClientBuilder
                .baseUrl(apiBase)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
        this.from = from;
        this.enabled = enabled;
    }

    @Override
    public void sendConfirmationEmail(
            String email,
            String animeTitle,
            String countryName,
            String confirmUrl) {
        String safeAnimeTitle = escapeHtml(animeTitle);
        String safeCountryName = escapeHtml(countryName);
        String safeConfirmUrl = escapeHtml(confirmUrl);

        String subject = "Confirma tu alerta de " + animeTitle;
        String html = """
                <p>Hola.</p>
                <p>Confirma tu alerta para saber cuándo <strong>%s</strong> llegue a plataformas de %s.</p>
                <p><a href="%s">Confirmar alerta</a></p>
                <p>El enlace caduca en 15 minutos.</p>
                """.formatted(safeAnimeTitle, safeCountryName, safeConfirmUrl);
        String text = """
                Confirma tu alerta de %s en %s:
                %s

                El enlace caduca en 15 minutos.
                """.formatted(animeTitle, countryName, confirmUrl).trim();

        send(email, subject, html, text);
    }

    @Override
    public void sendNewsletterConfirmationEmail(
            String email,
            String confirmUrl) {
        String safeConfirmUrl = escapeHtml(confirmUrl);

        String subject = "Confirma tu newsletter de DondeAnime";
        String html = """
                <p>Hola.</p>
                <p>Confirma tu suscripcion a la newsletter de DondeAnime.</p>
                <p><a href="%s">Confirmar newsletter</a></p>
                <p>El enlace caduca en 15 minutos.</p>
                """.formatted(safeConfirmUrl);
        String text = """
                Confirma tu suscripcion a la newsletter de DondeAnime:
                %s

                El enlace caduca en 15 minutos.
                """.formatted(confirmUrl).trim();

        send(email, subject, html, text);
    }

    @Override
    public void sendAlertEmail(
            String email,
            String animeTitle,
            String countryName,
            List<String> providerNames,
            String unsubscribeUrl,
            String eraseUrl) {
        String providers = String.join(", ", providerNames);
        String safeAnimeTitle = escapeHtml(animeTitle);
        String safeCountryName = escapeHtml(countryName);
        String safeProviders = escapeHtml(providers);
        String safeUnsubscribeUrl = escapeHtml(unsubscribeUrl);
        String safeEraseUrl = escapeHtml(eraseUrl);

        String subject = animeTitle + " ya está disponible en " + countryName;
        String html = """
                <p>Buenas noticias.</p>
                <p><strong>%s</strong> ya aparece disponible en %s.</p>
                <p>Plataformas detectadas: <strong>%s</strong>.</p>
                <p><a href="%s">Darme de baja de las alertas</a></p>
                <p><a href="%s">Borrar mis datos</a></p>
                """.formatted(safeAnimeTitle, safeCountryName, safeProviders, safeUnsubscribeUrl, safeEraseUrl);
        String text = """
                %s ya está disponible en %s.
                Plataformas detectadas: %s

                Baja: %s
                Borrar mis datos: %s
                """.formatted(animeTitle, countryName, providers, unsubscribeUrl, eraseUrl).trim();

        send(email, subject, html, text);
    }

    @Override
    public void sendPremiumWelcomeEmail(String email, String planTier, String manageUrl) {
        String safePlanTier = escapeHtml(planTier);
        String safeManageUrl = escapeHtml(manageUrl);
        String subject = "Premium DondeAnime activado";
        String html = """
                <p>Hola.</p>
                <p>Tu plan <strong>%s</strong> de DondeAnime ya esta activo.</p>
                <p>Puedes gestionar la suscripcion, tarjeta y facturas desde <a href="%s">tu panel Premium</a>.</p>
                """.formatted(safePlanTier, safeManageUrl);
        String text = """
                Tu plan %s de DondeAnime ya esta activo.

                Gestion: %s
                """.formatted(planTier, manageUrl).trim();

        send(email, subject, html, text);
    }

    @Override
    public void sendPremiumReceiptEmail(String email, String planTier, String paidAt, String manageUrl) {
        String safePlanTier = escapeHtml(planTier);
        String safePaidAt = escapeHtml(paidAt);
        String safeManageUrl = escapeHtml(manageUrl);
        String subject = "Recibo Premium DondeAnime";
        String html = """
                <p>Hola.</p>
                <p>Hemos registrado el pago de tu plan <strong>%s</strong>.</p>
                <p>Fecha registrada: <strong>%s</strong>.</p>
                <p>Stripe guarda el recibo fiscal completo en <a href="%s">tu portal de cliente</a>.</p>
                """.formatted(safePlanTier, safePaidAt, safeManageUrl);
        String text = """
                Hemos registrado el pago de tu plan %s.
                Fecha registrada: %s

                Recibo y gestion: %s
                """.formatted(planTier, paidAt, manageUrl).trim();

        send(email, subject, html, text);
    }

    @Override
    public void sendPremiumCancellationEmail(String email, String planTier, String premiumUrl) {
        String safePlanTier = escapeHtml(planTier);
        String safePremiumUrl = escapeHtml(premiumUrl);
        String subject = "Vuelve cuando quieras a Premium";
        String html = """
                <p>Hola.</p>
                <p>Hace unas semanas se cancelo tu plan <strong>%s</strong> de DondeAnime.</p>
                <p>Gracias por haber apoyado el proyecto. Si algun dia quieres volver, puedes hacerlo desde <a href="%s">Premium DondeAnime</a>.</p>
                """.formatted(safePlanTier, safePremiumUrl);
        String text = """
                Hace unas semanas se cancelo tu plan %s de DondeAnime.
                Gracias por haber apoyado el proyecto. Vuelve cuando quieras:

                %s
                """.formatted(planTier, premiumUrl).trim();

        send(email, subject, html, text);
    }

    @Override
    public void sendPremiumPortalEmail(String email, String planTier, String portalUrl) {
        String safePlanTier = escapeHtml(planTier);
        String safePortalUrl = escapeHtml(portalUrl);
        String subject = "Gestiona tu suscripcion Premium DondeAnime";
        String html = """
                <p>Hola.</p>
                <p>Has pedido gestionar tu plan <strong>%s</strong> de DondeAnime.</p>
                <p>Abre tu <a href="%s">portal de cliente seguro</a> para cambiar tarjeta, ver facturas o cancelar. El enlace caduca pronto.</p>
                <p>Si no has solicitado esto, ignora este correo.</p>
                """.formatted(safePlanTier, safePortalUrl);
        String text = """
                Has pedido gestionar tu plan %s de DondeAnime.

                Portal de cliente: %s

                Si no has solicitado esto, ignora este correo.
                """.formatted(planTier, portalUrl).trim();

        send(email, subject, html, text);
    }

    @Override
    public void sendPremiumAccessEmail(String email, String planTier, String accessUrl) {
        String safePlanTier = escapeHtml(planTier);
        String safeAccessUrl = escapeHtml(accessUrl);
        String subject = "Activa Premium en DondeAnime";
        String html = """
                <p>Hola.</p>
                <p>Has pedido activar tu plan <strong>%s</strong> en este navegador.</p>
                <p>Abre <a href="%s">este enlace seguro</a> para activar la sesion Premium. Caduca automaticamente.</p>
                <p>Si no has solicitado esto, ignora este correo.</p>
                """.formatted(safePlanTier, safeAccessUrl);
        String text = """
                Has pedido activar tu plan %s en este navegador.

                Enlace seguro: %s

                Si no has solicitado esto, ignora este correo.
                """.formatted(planTier, accessUrl).trim();

        send(email, subject, html, text);
    }

    private void send(String email, String subject, String html, String text) {
        if (!enabled) {
            log.info("Resend desactivado: email a '{}' con asunto '{}' no enviado", email, subject);
            return;
        }

        ResendEmailResponse response = restClient.post()
                .uri("/emails")
                .body(new ResendEmailRequest(from, List.of(email), subject, html, text))
                .retrieve()
                .body(ResendEmailResponse.class);

        log.info("Email enviado via Resend a '{}', id={}", email, response == null ? "unknown" : response.id());
    }

    private static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
