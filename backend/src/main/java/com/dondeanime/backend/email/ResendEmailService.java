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
    private final String siteUrl;

    public ResendEmailService(
            RestClient.Builder restClientBuilder,
            @Value("${resend.api-base}") String apiBase,
            @Value("${resend.api-key:}") String apiKey,
            @Value("${resend.from}") String from,
            @Value("${resend.enabled:false}") boolean enabled,
            @Value("${dondeanime.site-url:https://dondeanime.com}") String siteUrl) {
        this.restClient = restClientBuilder
                .baseUrl(apiBase)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
        this.from = from;
        this.enabled = enabled;
        this.siteUrl = siteUrl.endsWith("/") ? siteUrl.substring(0, siteUrl.length() - 1) : siteUrl;
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
                .body(new ResendEmailRequest(from, List.of(email), subject, wrapHtml(html), text))
                .retrieve()
                .body(ResendEmailResponse.class);

        log.info("Email enviado via Resend a '{}', id={}", email, response == null ? "unknown" : response.id());
    }

    /**
     * Envuelve el cuerpo de cada email en una plantilla de marca: cabecera con la
     * mascota, tarjeta clara con el contenido y pie con el enlace al sitio. La
     * maquetacion es a base de tablas e inline styles por compatibilidad con
     * clientes de correo (Outlook incluido); la cabecera se sirve como imagen
     * alojada en el sitio.
     */
    private String wrapHtml(String bodyHtml) {
        // Los enlaces del cuerpo llevan color de marca inline (no por <style>, que Gmail
        // elimina) para no salir en azul por defecto ni perder contraste en modo oscuro.
        String styledBody = bodyHtml.replace(
                "<a href=", "<a style=\"color:#B83A14;text-decoration:underline;\" href=");
        return """
                <!DOCTYPE html>
                <html lang="es">
                <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <meta name="color-scheme" content="light">
                <meta name="supported-color-schemes" content="light">
                </head>
                <body style="margin:0;padding:0;background-color:#0E1020;color-scheme:light;">
                  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="background-color:#0E1020;">
                    <tr><td align="center" style="padding:24px 12px;">
                      <table role="presentation" width="600" cellpadding="0" cellspacing="0" border="0" style="width:600px;max-width:600px;background-color:#ffffff;border-radius:16px;overflow:hidden;">
                        <tr><td style="padding:0;background-color:#0E1020;">
                          <a href="%1$s" style="text-decoration:none;">
                            <img src="%1$s/brand/email-masthead.jpg" width="600" alt="DondeAnime" style="display:block;width:100%%;max-width:600px;height:auto;border:0;font-family:Arial,Helvetica,sans-serif;font-size:22px;font-weight:bold;color:#F4F5FB;">
                          </a>
                        </td></tr>
                        <tr><td style="padding:28px 32px;font-family:Arial,Helvetica,sans-serif;font-size:16px;line-height:1.6;color:#19151F;">
                          %2$s
                        </td></tr>
                        <tr><td style="padding:18px 32px;border-top:1px solid #ECE7E0;font-family:Arial,Helvetica,sans-serif;font-size:12px;line-height:1.5;color:#625B69;">
                          DondeAnime &middot; Encuentra donde ver cada anime.<br>
                          <a href="%1$s" style="color:#B83A14;text-decoration:none;">dondeanime.com</a>
                        </td></tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(siteUrl, styledBody);
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
