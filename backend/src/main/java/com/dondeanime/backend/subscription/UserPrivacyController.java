package com.dondeanime.backend.subscription;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserPrivacyController {

    private final SubscriptionService subscriptionService;

    public UserPrivacyController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @DeleteMapping("/{email}/erase")
    public ResponseEntity<Void> erase(@PathVariable String email, @RequestParam String token) {
        subscriptionService.eraseUser(email, token);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/{email}/erase", produces = MediaType.TEXT_HTML_VALUE)
    public String erasePage(@PathVariable String email, @RequestParam String token) {
        String escapedEmail = escapeHtml(email);
        String escapedToken = escapeHtml(token);
        return """
                <!doctype html>
                <html lang="es">
                  <head>
                    <meta charset="utf-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1">
                    <title>Borrar datos | DondeAnime</title>
                  </head>
                  <body style="font-family: system-ui, sans-serif; max-width: 42rem; margin: 4rem auto; padding: 0 1rem;">
                    <h1>Borrar datos</h1>
                    <p>Esto borrará el email %s y sus alertas asociadas.</p>
                    <button type="button" id="erase">Borrar mis datos</button>
                    <p id="status" aria-live="polite"></p>
                    <script>
                      document.getElementById('erase').addEventListener('click', async () => {
                        const res = await fetch('/api/users/%s/erase?token=%s', { method: 'DELETE' });
                        document.getElementById('status').textContent = res.ok
                          ? 'Datos borrados.'
                          : 'No se pudieron borrar los datos.';
                      });
                    </script>
                  </body>
                </html>
                """.formatted(escapedEmail, encodePathSegment(email), escapedToken);
    }

    private static String encodePathSegment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
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
