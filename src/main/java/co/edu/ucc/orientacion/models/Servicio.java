package co.edu.ucc.orientacion.models;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Servicio institucional de bienestar o dependencia del directorio institucional.
 *
 * @author Gabriela Zabaleta
 * @param id identificador único UUID
 * @param nombre nombre del servicio o dependencia
 * @param descripcion descripción del servicio
 * @param categoria categoría del servicio
 * @param edificio edificio donde se presta
 * @param horario horario de atención
 * @param contacto datos de contacto
 * @param activo indica si el servicio está vigente
 * @param creadoEn fecha de creación
 */
public record Servicio(
        UUID id,
        String nombre,
        String descripcion,
        String categoria,
        String edificio,
        String horario,
        String contacto,
        boolean activo,
        LocalDateTime creadoEn) {
}
