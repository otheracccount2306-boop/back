package co.edu.ucc.orientacion.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Solicitud de recuperación de contraseña.
 *
 * @author Doris Arzuaga
 * @param correo correo del usuario que olvidó su contraseña
 */
public record PasswordRecoveryRequest(@NotBlank @Email String correo) {
}
