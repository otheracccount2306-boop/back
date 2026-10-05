package co.edu.ucc.orientacion.models;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Espacio físico del campus: aula, laboratorio, oficina, biblioteca, cafetería o área común.
 *
 * @author Diego Luna
 * @param id identificador único UUID
 * @param nombre nombre del espacio
 * @param codigo código único del espacio
 * @param categoria categoría del espacio
 * @param edificio edificio donde se ubica
 * @param piso piso donde se ubica
 * @param descripcion descripción del espacio
 * @param referencia indicaciones de referencia para llegar
 * @param activo indica si el espacio está vigente
 * @param creadoEn fecha de creación
 */
public record Espacio(
        UUID id,
        String nombre,
        String codigo,
        String categoria,
        String edificio,
        String piso,
        String descripcion,
        String referencia,
        boolean activo,
        LocalDateTime creadoEn) {
}
