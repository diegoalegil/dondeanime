package com.dondeanime.backend.newsletter;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/newsletter")
public class NewsletterController {

    private final NewsletterService newsletterService;

    public NewsletterController(NewsletterService newsletterService) {
        this.newsletterService = newsletterService;
    }

    @PostMapping("/subscribe")
    public ResponseEntity<NewsletterResponse> subscribe(@Valid @RequestBody NewsletterSubscribeRequest request) {
        newsletterService.requestSubscription(request);
        return ResponseEntity.accepted()
                .body(new NewsletterResponse("Si el email es valido, recibiras un correo de confirmacion."));
    }

    @GetMapping(value = "/confirm", produces = MediaType.TEXT_HTML_VALUE)
    public String confirm(@RequestParam String token) {
        String email = newsletterService.confirmSubscription(token);
        return htmlPage(
                "Newsletter confirmada",
                "Tu suscripcion a la newsletter de DondeAnime queda activa para " + email + ".");
    }

    private static String htmlPage(String title, String body) {
        return """
                <!doctype html>
                <html lang="es">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>%s</title>
                </head>
                <body style="font-family: system-ui, sans-serif; margin: 2rem; line-height: 1.5;">
                  <h1>%s</h1>
                  <p>%s</p>
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

}
