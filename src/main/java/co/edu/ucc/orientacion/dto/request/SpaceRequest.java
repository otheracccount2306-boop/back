package co.edu.ucc.orientacion.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SpaceRequest(
        @NotBlank @Size(max = 150) String nombre,
        @NotBlank @Size(max = 30) String codigo,
        @NotBlank @Size(max = 50) String categoria,
        @Size(max = 100) String edificio,
        @Size(max = 30) String piso,
        String descripcion,
        String referencia,
        Boolean activo) {
}
