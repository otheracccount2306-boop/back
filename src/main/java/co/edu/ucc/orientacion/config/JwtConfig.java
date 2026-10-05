package co.edu.ucc.orientacion.config;

import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * Parámetros de firma y vigencia de los tokens JWT, leídos desde application.properties.
 *
 * @author Doris Arzuaga
 * @author Diego Luna
 * @author Gabriela Zabaleta
 */
@Configuration
public class JwtConfig {

    private final SecretKey signingKey;
    private final long expiration;
    private final long refreshExpiration;

    /**
     * Crea la configuración JWT.
     *
     * @author Doris Arzuaga
     * @author Diego Luna
     * @author Gabriela Zabaleta
     * @param secret secreto de firma, de al menos 32 caracteres
     * @param expiration vigencia del access token en milisegundos
     * @param refreshExpiration vigencia del refresh token en milisegundos
     */
    public JwtConfig(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expiration,
            @Value("${jwt.refresh-expiration}") long refreshExpiration) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
        this.refreshExpiration = refreshExpiration;
    }

    /**
     * Obtiene la clave HMAC usada para firmar y verificar los tokens.
     *
     * @author Doris Arzuaga
     * @author Diego Luna
     * @author Gabriela Zabaleta
     * @return clave de firma
     */
    public SecretKey getSigningKey() {
        return signingKey;
    }

    /**
     * Obtiene la vigencia del access token.
     *
     * @author Doris Arzuaga
     * @author Diego Luna
     * @author Gabriela Zabaleta
     * @return milisegundos de vigencia
     */
    public long getExpiration() {
        return expiration;
    }

    /**
     * Obtiene la vigencia del refresh token.
     *
     * @author Doris Arzuaga
     * @author Diego Luna
     * @author Gabriela Zabaleta
     * @return milisegundos de vigencia
     */
    public long getRefreshExpiration() {
        return refreshExpiration;
    }
}
