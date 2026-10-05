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

/**
 * Generación y validación de tokens JWT de acceso y de refresco.
 *
 * @author Doris Arzuaga
 * @author Diego Luna
 * @author Gabriela Zabaleta
 */
@Component
public class JwtUtils {

    public static final String CLAIM_ROLE = "rol";
    public static final String CLAIM_TYPE = "type";
    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private final JwtConfig config;

    /**
     * Crea el componente con la configuración JWT.
     *
     * @author Doris Arzuaga
     * @author Diego Luna
     * @author Gabriela Zabaleta
     * @param config parámetros de firma y vigencia
     */
    public JwtUtils(JwtConfig config) {
        this.config = config;
    }

    /**
     * Genera un access token con el rol del usuario.
     *
     * @author Doris Arzuaga
     * @author Diego Luna
     * @author Gabriela Zabaleta
     * @param userId identificador del usuario
     * @param rol rol del usuario
     * @return JWT firmado con vigencia de acceso
     */
    public String generateAccessToken(UUID userId, String rol) {
        return build(userId, TYPE_ACCESS, config.getExpiration(), rol);
    }

    /**
     * Genera un refresh token.
     *
     * @author Doris Arzuaga
     * @author Diego Luna
     * @author Gabriela Zabaleta
     * @param userId identificador del usuario
     * @return JWT firmado con vigencia de refresco
     */
    public String generateRefreshToken(UUID userId) {
        return build(userId, TYPE_REFRESH, config.getRefreshExpiration(), null);
    }

    /**
     * Valida un access token verificando firma, expiración y tipo.
     *
     * @author Doris Arzuaga
     * @author Diego Luna
     * @author Gabriela Zabaleta
     * @param token JWT recibido
     * @return claims del token, o vacío si no es un access token válido
     */
    public Optional<Claims> parseAccessToken(String token) {
        return parse(token, TYPE_ACCESS);
    }

    /**
     * Valida un refresh token verificando firma, expiración y tipo.
     *
     * @author Doris Arzuaga
     * @author Diego Luna
     * @author Gabriela Zabaleta
     * @param token JWT recibido
     * @return claims del token, o vacío si no es un refresh token válido
     */
    public Optional<Claims> parseRefreshToken(String token) {
        return parse(token, TYPE_REFRESH);
    }

    /**
     * Calcula la fecha de expiración de un refresh token emitido en este momento.
     *
     * @author Doris Arzuaga
     * @author Diego Luna
     * @author Gabriela Zabaleta
     * @return fecha y hora de expiración
     */
    public LocalDateTime refreshExpiresAt() {
        return LocalDateTime.now().plus(Duration.ofMillis(config.getRefreshExpiration()));
    }

    /**
     * Obtiene la vigencia del access token en segundos.
     *
     * @author Doris Arzuaga
     * @author Diego Luna
     * @author Gabriela Zabaleta
     * @return segundos de vigencia
     */
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
