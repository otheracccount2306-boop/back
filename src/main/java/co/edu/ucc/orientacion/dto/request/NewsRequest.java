package co.edu.ucc.orientacion.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record NewsRequest(
        @NotBlank @Size(max = 250) String titulo,
        @Size(max = 500) String resumen,
        @NotBlank String contenido,
        @NotBlank @Size(max = 50) String categoria,
        @Size(max = 500) @Pattern(regexp = "^https?://.+$", message = "debe ser una URL http o https") String imagenUrl,
        @Pattern(regexp = "(?i)BORRADOR|PUBLICADO", message = "debe ser BORRADOR o PUBLICADO") String estado) {
}
