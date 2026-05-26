package com.dondeanime.backend.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.dondeanime.backend.anime.Anime;

class EmailTokenServiceTest {

    private final EmailTokenRepository repository = org.mockito.Mockito.mock(EmailTokenRepository.class);
    private final JwtTokenService jwtTokenService = org.mockito.Mockito.mock(JwtTokenService.class);
    private final EmailTokenService service = new EmailTokenService(repository, jwtTokenService);

    @Test
    void createConfirmationTokenStoresHashedToken() {
        AppUser user = user();
        Anime anime = anime();
        when(jwtTokenService.createToken(EmailToken.TYPE_CONFIRMATION, Duration.ofMinutes(15))).thenReturn("raw.jwt");
        when(jwtTokenService.hash("raw.jwt")).thenReturn("hash");
        when(repository.save(any(EmailToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IssuedEmailToken issued = service.createConfirmationToken(user, anime, "ES");

        assertThat(issued.rawToken()).isEqualTo("raw.jwt");
        assertThat(issued.entity().getUser()).isSameAs(user);
        assertThat(issued.entity().getAnime()).isSameAs(anime);
        assertThat(issued.entity().getCountryCode()).isEqualTo("ES");
        assertThat(issued.entity().getTokenType()).isEqualTo(EmailToken.TYPE_CONFIRMATION);
        assertThat(issued.entity().getTokenHash()).isEqualTo("hash");
        assertThat(issued.entity().getExpiresAt()).isAfter(issued.entity().getCreatedAt());
    }

    @Test
    void consumeConfirmationTokenMarksItAsUsed() {
        EmailToken token = token(EmailToken.TYPE_CONFIRMATION, Instant.now().plusSeconds(60), null);
        when(jwtTokenService.hasValidSignature("raw.jwt")).thenReturn(true);
        when(jwtTokenService.hash("raw.jwt")).thenReturn("hash");
        when(repository.findByTokenHash("hash")).thenReturn(Optional.of(token));
        when(repository.save(token)).thenReturn(token);

        EmailToken consumed = service.consumeConfirmationToken("raw.jwt");

        assertThat(consumed.getUsedAt()).isNotNull();
    }

    @Test
    void resolveTokenRejectsInvalidExpiredUsedOrWrongTypeTokens() {
        when(jwtTokenService.hasValidSignature("bad.jwt")).thenReturn(false);
        assertStatus(
                () -> service.consumeConfirmationToken("bad.jwt"),
                HttpStatus.BAD_REQUEST);

        when(jwtTokenService.hasValidSignature("expired.jwt")).thenReturn(true);
        when(jwtTokenService.hash("expired.jwt")).thenReturn("expired");
        when(repository.findByTokenHash("expired"))
                .thenReturn(Optional.of(token(EmailToken.TYPE_CONFIRMATION, Instant.now().minusSeconds(1), null)));
        assertStatus(
                () -> service.consumeConfirmationToken("expired.jwt"),
                HttpStatus.GONE);

        when(jwtTokenService.hasValidSignature("used.jwt")).thenReturn(true);
        when(jwtTokenService.hash("used.jwt")).thenReturn("used");
        when(repository.findByTokenHash("used"))
                .thenReturn(Optional.of(token(
                        EmailToken.TYPE_CONFIRMATION,
                        Instant.now().plusSeconds(60),
                        Instant.now())));
        assertStatus(
                () -> service.consumeConfirmationToken("used.jwt"),
                HttpStatus.GONE);

        when(jwtTokenService.hasValidSignature("wrong.jwt")).thenReturn(true);
        when(jwtTokenService.hash("wrong.jwt")).thenReturn("wrong");
        when(repository.findByTokenHash("wrong"))
                .thenReturn(Optional.of(token(EmailToken.TYPE_UNSUBSCRIBE, Instant.now().plusSeconds(60), null)));
        assertStatus(
                () -> service.consumeConfirmationToken("wrong.jwt"),
                HttpStatus.BAD_REQUEST);
    }

    private static void assertStatus(Runnable call, HttpStatus status) {
        assertThatThrownBy(call::run)
                .isInstanceOfSatisfying(ResponseStatusException.class, ex ->
                        assertThat(ex.getStatusCode()).isEqualTo(status));
    }

    private static AppUser user() {
        AppUser user = new AppUser();
        user.setEmail("diego@example.com");
        user.setCreatedAt(Instant.now());
        return user;
    }

    private static Anime anime() {
        Anime anime = new Anime();
        anime.setSlug("attack-on-titan");
        return anime;
    }

    private static EmailToken token(String type, Instant expiresAt, Instant usedAt) {
        EmailToken token = new EmailToken();
        token.setUser(user());
        token.setAnime(anime());
        token.setCountryCode("ES");
        token.setTokenType(type);
        token.setTokenHash("hash");
        token.setCreatedAt(Instant.now());
        token.setExpiresAt(expiresAt);
        token.setUsedAt(usedAt);
        return token;
    }
}
