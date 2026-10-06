package co.edu.ucc.orientacion.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ServiceRequest(
        @NotBlank @Size(max = 150) String nombre,
        String descripcion,
        @Size(max = 50) String categoria,
        @Size(max = 100) String edificio,
        @Size(max = 200) String horario,
        @Size(max = 150) String contacto,
        Boolean activo) {
}
