package co.edu.ucc.orientacion.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PlanRequest(
        @NotBlank @Size(max = 150) String nombre,
        @Size(max = 100) String edificio,
        @Size(max = 30) String piso,
        @Size(max = PlanRequest.MAX_IMAGE_CHARS, message = "La imagen supera el tamaño máximo de 2 MB") String imagen,
        @Min(1) @Max(8192) Integer ancho,
        @Min(1) @Max(8192) Integer alto,
        Boolean activo) {

    public static final int MAX_IMAGE_CHARS = 2_800_000;
}
