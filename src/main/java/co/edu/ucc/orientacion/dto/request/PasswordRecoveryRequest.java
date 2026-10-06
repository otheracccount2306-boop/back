package co.edu.ucc.orientacion.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PasswordRecoveryRequest(@NotBlank @Email String correo) {
}
