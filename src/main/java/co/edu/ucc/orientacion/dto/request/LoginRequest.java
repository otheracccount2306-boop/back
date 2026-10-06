package co.edu.ucc.orientacion.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String correo,
        @NotBlank String contrasena) {
}
