package co.edu.ucc.orientacion.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Solicitud para obtener un nuevo access token a partir de un refresh token.
 *
 * @author Doris Arzuaga
 * @param refreshToken refresh token emitido durante el inicio de sesión
 */
public record RefreshRequest(@NotBlank String refreshToken) {
}
