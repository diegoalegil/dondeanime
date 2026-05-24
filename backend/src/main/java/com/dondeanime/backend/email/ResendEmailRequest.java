package com.dondeanime.backend.email;

import java.util.List;

public record ResendEmailRequest(
        String from,
        List<String> to,
        String subject,
        String html,
        String text
) {}
