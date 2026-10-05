package co.edu.ucc.orientacion.dto.response;

/**
 * Respuesta de autenticación con los tokens emitidos y los datos del usuario.
 *
 * @author Doris Arzuaga
 * @param accessToken JWT de acceso con vigencia de 24 horas
 * @param refreshToken JWT de refresco con vigencia de 7 días
 * @param tokenType tipo de token para el encabezado Authorization
 * @param expiresIn segundos de vigencia del access token
 * @param usuario datos del usuario autenticado
 */
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UserResponse usuario) {
}
