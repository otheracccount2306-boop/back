package co.edu.ucc.orientacion.security;

import co.edu.ucc.orientacion.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Component
public class JwtUtils {

    public static final String CLAIM_ROLE = "rol";
    public static final String CLAIM_TYPE = "type";
    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private final JwtConfig config;

    public JwtUtils(JwtConfig config) {
        this.config = config;
    }

    public String generateAccessToken(UUID userId, String rol) {
        return build(userId, TYPE_ACCESS, config.getExpiration(), rol);
    }

    public String generateRefreshToken(UUID userId) {
        return build(userId, TYPE_REFRESH, config.getRefreshExpiration(), null);
    }

    public Optional<Claims> parseAccessToken(String token) {
        return parse(token, TYPE_ACCESS);
    }

    public Optional<Claims> parseRefreshToken(String token) {
        return parse(token, TYPE_REFRESH);
    }

    public LocalDateTime refreshExpiresAt() {
        return LocalDateTime.now().plus(Duration.ofMillis(config.getRefreshExpiration()));
    }

    public long getExpirationSeconds() {
        return config.getExpiration() / 1000;
    }

    private String build(UUID userId, String type, long ttlMillis, String rol) {
        Date now = new Date();
        JwtBuilder builder = Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(userId.toString())
                .claim(CLAIM_TYPE, type)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttlMillis))
                .signWith(config.getSigningKey());
        if (rol != null) {
            builder.claim(CLAIM_ROLE, rol);
        }
        return builder.compact();
    }

    private Optional<Claims> parse(String token, String expectedType) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(config.getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return expectedType.equals(claims.get(CLAIM_TYPE, String.class))
                    ? Optional.of(claims)
                    : Optional.empty();
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
