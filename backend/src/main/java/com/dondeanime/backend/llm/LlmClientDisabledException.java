package com.dondeanime.backend.llm;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class LlmClientDisabledException extends IllegalStateException {

    public LlmClientDisabledException() {
        super("LLM desactivado. Configura LLM_ENABLED=true y ANTHROPIC_API_KEY.");
    }
}
