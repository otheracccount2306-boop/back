package co.edu.ucc.orientacion.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateRoleRequest(@NotBlank String rol) {
}
