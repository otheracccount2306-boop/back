package co.edu.ucc.orientacion.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Credenciales para iniciar sesión.
 *
 * @author Doris Arzuaga
 * @param correo correo institucional del usuario
 * @param contrasena contraseña en texto plano
 */
public record LoginRequest(
        @NotBlank String correo,
        @NotBlank String contrasena) {
}
