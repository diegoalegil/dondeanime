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
