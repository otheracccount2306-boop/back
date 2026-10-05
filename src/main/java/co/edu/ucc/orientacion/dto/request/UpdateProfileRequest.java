package co.edu.ucc.orientacion.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Campos editables del perfil. El correo y el rol son de solo lectura.
 *
 * @author Doris Arzuaga
 * @param nombre nombres del usuario
 * @param apellido apellidos del usuario
 * @param programaAcademico programa académico cursado
 * @param telefono teléfono de contacto
 */
public record UpdateProfileRequest(
        @NotBlank @Size(max = 100) String nombre,
        @NotBlank @Size(max = 100) String apellido,
        @Size(max = 150) String programaAcademico,
        @Pattern(regexp = "^[0-9+()\\- ]{7,20}$", message = "formato de teléfono inválido") String telefono) {
}
