package co.edu.ucc.orientacion.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank @Size(max = 100) String nombre,
        @NotBlank @Size(max = 100) String apellido,
        @Size(max = 150) String programaAcademico,
        @Pattern(regexp = "^[0-9+()\\- ]{7,20}$", message = "formato de teléfono inválido") String telefono) {
}
