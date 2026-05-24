package com.dondeanime.backend.subscription;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PostMapping
    public ResponseEntity<SubscriptionResponse> create(@Valid @RequestBody SubscriptionRequest request) {
        subscriptionService.requestSubscription(request);
        return ResponseEntity.accepted().body(new SubscriptionResponse(
                "Si el email es válido, recibirás un correo de confirmación."));
    }

    @GetMapping(value = "/confirm", produces = MediaType.TEXT_HTML_VALUE)
    public String confirm(@RequestParam String token) {
        ConfirmedSubscription confirmed = subscriptionService.confirmSubscription(token);
        return htmlPage(
                "Alerta confirmada",
                "Te avisaremos cuando " + confirmed.animeTitle() + " llegue a plataformas de "
                        + confirmed.countryName() + ".");
    }

    @GetMapping(value = "/unsubscribe", produces = MediaType.TEXT_HTML_VALUE)
    public String unsubscribePage(@RequestParam String token) {
        String action = "/api/subscriptions/unsubscribe?token=" + encode(token);
        return """
                <!doctype html>
                <html lang="es">
                  <head>
                    <meta charset="utf-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1">
                    <title>Darse de baja | DondeAnime</title>
                  </head>
                  <body style="font-family: system-ui, sans-serif; max-width: 42rem; margin: 4rem auto; padding: 0 1rem;">
                    <h1>Darse de baja</h1>
                    <p>Confirma que quieres dejar de recibir alertas de DondeAnime.</p>
                    <form method="post" action="%s">
                      <button type="submit">Darme de baja</button>
                    </form>
                  </body>
                </html>
                """.formatted(escapeHtml(action));
    }

    @PostMapping(value = "/unsubscribe", produces = MediaType.TEXT_HTML_VALUE)
    public String unsubscribe(@RequestParam String token) {
        ConfirmedSubscription unsubscribed = subscriptionService.unsubscribe(token);
        return htmlPage(
                "Baja completada",
                "No enviaremos más alertas a " + unsubscribed.email() + ".");
    }

    private static String htmlPage(String title, String body) {
        return """
                <!doctype html>
                <html lang="es">
                  <head>
                    <meta charset="utf-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1">
                    <title>%s | DondeAnime</title>
                  </head>
                  <body style="font-family: system-ui, sans-serif; max-width: 42rem; margin: 4rem auto; padding: 0 1rem;">
                    <h1>%s</h1>
                    <p>%s</p>
                    <p><a href="https://dondeanime.com">Volver a DondeAnime</a></p>
                  </body>
                </html>
                """.formatted(escapeHtml(title), escapeHtml(title), escapeHtml(body));
    }

    private static String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
