package com.dondeanime.backend.embedding;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.springframework.stereotype.Component;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class EmbeddingContentHasher {

    private static final String SHA_256 = "SHA-256";

    private final ObjectMapper objectMapper;

    public EmbeddingContentHasher() {
        this(new ObjectMapper());
    }

    EmbeddingContentHasher(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String hash(AnimeSearchDocument document) {
        try {
            byte[] json = objectMapper.writeValueAsString(document).getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(MessageDigest.getInstance(SHA_256).digest(json));
        } catch (JacksonException | NoSuchAlgorithmException e) {
            throw new IllegalStateException("No se pudo calcular hash de documento embedding", e);
        }
    }
}
