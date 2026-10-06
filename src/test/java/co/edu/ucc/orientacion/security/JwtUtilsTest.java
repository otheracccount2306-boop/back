package co.edu.ucc.orientacion.security;

import co.edu.ucc.orientacion.config.JwtConfig;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilsTest {

    private static final String SECRET = "ucc_orientacion_jwt_secret_2026_spring";

    private final JwtUtils jwtUtils = new JwtUtils(new JwtConfig(SECRET, 86_400_000L, 604_800_000L));

    @Test
    @DisplayName("Un access token válido se acepta y conserva el usuario y el rol")
    void accessTokenIsAccepted() {
        UUID userId = UUID.randomUUID();
        String token = jwtUtils.generateAccessToken(userId, "ADMINISTRADOR");

        Optional<Claims> claims = jwtUtils.parseAccessToken(token);

        assertTrue(claims.isPresent());
        assertEquals(userId.toString(), claims.get().getSubject());
        assertEquals("ADMINISTRADOR", claims.get().get(JwtUtils.CLAIM_ROLE, String.class));
    }

    @Test
    @DisplayName("Un refresh token no puede usarse como access token")
    void refreshTokenIsRejectedAsAccessToken() {
        String refresh = jwtUtils.generateRefreshToken(UUID.randomUUID());

        assertTrue(jwtUtils.parseAccessToken(refresh).isEmpty());
        assertTrue(jwtUtils.parseRefreshToken(refresh).isPresent());
    }

    @Test
    @DisplayName("Un access token no puede usarse como refresh token")
    void accessTokenIsRejectedAsRefreshToken() {
        String access = jwtUtils.generateAccessToken(UUID.randomUUID(), "ESTUDIANTE");

        assertTrue(jwtUtils.parseRefreshToken(access).isEmpty());
    }

    @Test
    @DisplayName("Un token alterado se rechaza")
    void tamperedTokenIsRejected() {
        String token = jwtUtils.generateAccessToken(UUID.randomUUID(), "ESTUDIANTE");
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertTrue(jwtUtils.parseAccessToken(tampered).isEmpty());
    }

    @Test
    @DisplayName("Un token firmado con otro secreto se rechaza")
    void tokenSignedWithAnotherSecretIsRejected() {
        JwtUtils other = new JwtUtils(new JwtConfig("otro_secreto_distinto_para_pruebas_1234567890", 86_400_000L, 1L));
        String foreign = other.generateAccessToken(UUID.randomUUID(), "ESTUDIANTE");

        assertTrue(jwtUtils.parseAccessToken(foreign).isEmpty());
    }

    @Test
    @DisplayName("Un token expirado se rechaza")
    void expiredTokenIsRejected() {
        JwtUtils expired = new JwtUtils(new JwtConfig(SECRET, -1_000L, -1_000L));
        String token = expired.generateAccessToken(UUID.randomUUID(), "ESTUDIANTE");

        assertTrue(jwtUtils.parseAccessToken(token).isEmpty());
    }

    @Test
    @DisplayName("Un texto que no es un JWT se rechaza")
    void garbageIsRejected() {
        assertTrue(jwtUtils.parseAccessToken("no-es-un-jwt").isEmpty());
        assertTrue(jwtUtils.parseAccessToken("").isEmpty());
    }

    @Test
    @DisplayName("La vigencia del access token se expresa en segundos")
    void expirationIsExpressedInSeconds() {
        assertEquals(86_400L, jwtUtils.getExpirationSeconds());
    }
}
