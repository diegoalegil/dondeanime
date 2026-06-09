package com.dondeanime.backend.embedding;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class EmbeddingClientDisabledException extends IllegalStateException {

    public EmbeddingClientDisabledException() {
        super("Embeddings desactivados. Configura EMBEDDINGS_ENABLED=true y EMBEDDING_API_KEY.");
    }
}
