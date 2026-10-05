package co.edu.ucc.orientacion.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos para crear o actualizar un espacio del campus.
 *
 * @author Diego Luna
 * @param nombre nombre del espacio
 * @param codigo código único del espacio
 * @param categoria categoría: AULA, LABORATORIO, OFICINA, BIBLIOTECA, CAFETERIA o AREA_COMUN
 * @param edificio edificio donde se ubica
 * @param piso piso donde se ubica
 * @param descripcion descripción del espacio
 * @param referencia indicaciones de referencia para llegar
 * @param activo si el espacio es visible para los estudiantes; nulo conserva el estado actual
 */
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
