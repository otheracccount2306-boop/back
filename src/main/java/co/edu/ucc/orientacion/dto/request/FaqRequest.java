package co.edu.ucc.orientacion.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos para crear o actualizar una pregunta frecuente.
 *
 * @author Gabriela Zabaleta
 * @param pregunta texto de la pregunta
 * @param respuesta texto de la respuesta
 * @param categoria categoría de la pregunta
 * @param activo si la pregunta es visible para los estudiantes; nulo conserva el estado actual
 */
public record FaqRequest(
        @NotBlank @Size(max = 500) String pregunta,
        @NotBlank String respuesta,
        @NotBlank @Size(max = 50) String categoria,
        Boolean activo) {
}
