package co.edu.ucc.orientacion.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(@NotNull Boolean activo) {
}
