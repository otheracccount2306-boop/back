package co.edu.ucc.orientacion.models;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Pregunta frecuente con su respuesta.
 *
 * @author Gabriela Zabaleta
 * @param id identificador único UUID
 * @param pregunta texto de la pregunta
 * @param respuesta texto de la respuesta
 * @param categoria categoría de la pregunta
 * @param frecuencia contador de frecuencia de consulta
 * @param activo indica si la pregunta está vigente
 * @param creadoEn fecha de creación
 */
public record Faq(
        UUID id,
        String pregunta,
        String respuesta,
        String categoria,
        int frecuencia,
        boolean activo,
        LocalDateTime creadoEn) {
}
