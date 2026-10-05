package co.edu.ucc.orientacion.models;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Token de un solo uso para la recuperación de contraseña. Solo se almacena su hash SHA-256.
 *
 * @author Doris Arzuaga
 * @param id identificador único UUID
 * @param usuarioId usuario propietario del token
 * @param tokenHash hash SHA-256 del token entregado al usuario
 * @param expiraEn fecha de expiración del token
 * @param usado indica si el token ya fue consumido
 * @param creadoEn fecha de creación
 */
public record TokenRecuperacion(
        UUID id,
        UUID usuarioId,
        String tokenHash,
        LocalDateTime expiraEn,
        boolean usado,
        LocalDateTime creadoEn) {
}
