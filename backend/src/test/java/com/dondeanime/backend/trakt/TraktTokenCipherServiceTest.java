package com.dondeanime.backend.trakt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class TraktTokenCipherServiceTest {

    @Test
    void encryptsAndDecryptsTokenWithoutStoringPlaintext() {
        TraktTokenCipherService service = new TraktTokenCipherService("test-secret-with-enough-entropy");

        String ciphertext = service.encrypt(" access-token ");

        assertThat(ciphertext).startsWith("v1:");
        assertThat(ciphertext).doesNotContain("access-token");
        assertThat(service.decrypt(ciphertext)).isEqualTo("access-token");
    }

    @Test
    void rejectsUseWithoutSecret() {
        TraktTokenCipherService service = new TraktTokenCipherService("");

        assertThat(service.isConfigured()).isFalse();
        assertThatThrownBy(() -> service.encrypt("token"))
                .isInstanceOf(IllegalStateException.class);
    }
}
