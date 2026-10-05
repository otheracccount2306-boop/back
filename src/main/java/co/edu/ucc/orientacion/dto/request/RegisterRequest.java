package co.edu.ucc.orientacion.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos requeridos para el registro de un estudiante con consentimiento informado.
 *
 * @author Doris Arzuaga
 * @param nombre nombres del estudiante
 * @param apellido apellidos del estudiante
 * @param correo correo institucional
 * @param contrasena contraseña en texto plano de al menos 8 caracteres con letras y números
 * @param programaAcademico programa académico cursado
 * @param telefono teléfono de contacto
 * @param consentimientoDatos aceptación del tratamiento de datos personales (Ley 1581 de 2012)
 */
public record RegisterRequest(
        @NotBlank @Size(max = 100) String nombre,
        @NotBlank @Size(max = 100) String apellido,
        @NotBlank @Email @Size(max = 150) String correo,
        @NotBlank
        @Size(min = 8, max = 72)
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "debe contener letras y números")
        String contrasena,
        @Size(max = 150) String programaAcademico,
        @Pattern(regexp = "^[0-9+()\\- ]{7,20}$", message = "formato de teléfono inválido") String telefono,
        boolean consentimientoDatos) {
}
