package co.edu.ucc.orientacion.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

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
