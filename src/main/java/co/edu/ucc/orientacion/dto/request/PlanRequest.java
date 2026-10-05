package co.edu.ucc.orientacion.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos para crear o actualizar un plano del campus.
 *
 * @author Diego Luna
 * @param nombre nombre del plano
 * @param edificio edificio que representa
 * @param piso piso que representa
 * @param imagen imagen como data URL PNG, JPEG o WebP; obligatoria al crear y opcional al editar
 *               (null conserva la imagen actual)
 * @param ancho ancho de la imagen en píxeles; se usa solo si el servidor no puede leerlo de la imagen
 * @param alto alto de la imagen en píxeles; se usa solo si el servidor no puede leerlo de la imagen
 * @param activo si el plano es visible para los estudiantes; nulo conserva el estado actual
 */
public record PlanRequest(
        @NotBlank @Size(max = 150) String nombre,
        @Size(max = 100) String edificio,
        @Size(max = 30) String piso,
        @Size(max = PlanRequest.MAX_IMAGE_CHARS, message = "La imagen supera el tamaño máximo de 2 MB") String imagen,
        @Min(1) @Max(8192) Integer ancho,
        @Min(1) @Max(8192) Integer alto,
        Boolean activo) {

    /** Tamaño máximo del data URL: unos 2 MB de imagen en base64. Mantiene el plano cacheable en el móvil. */
    public static final int MAX_IMAGE_CHARS = 2_800_000;
}
