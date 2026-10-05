package co.edu.ucc.orientacion.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos para crear o actualizar un servicio de bienestar o una dependencia del directorio.
 * La categoría es obligatoria solo para servicios de bienestar.
 *
 * @author Gabriela Zabaleta
 * @param nombre nombre del servicio o dependencia
 * @param descripcion descripción del servicio
 * @param categoria categoría: PSICOLOGIA, SALUD, DEPORTE, CULTURA, PASTORAL o BECAS
 * @param edificio edificio donde se presta
 * @param horario horario de atención
 * @param contacto datos de contacto
 * @param activo si el servicio es visible para los estudiantes; nulo conserva el estado actual
 */
public record ServiceRequest(
        @NotBlank @Size(max = 150) String nombre,
        String descripcion,
        @Size(max = 50) String categoria,
        @Size(max = 100) String edificio,
        @Size(max = 200) String horario,
        @Size(max = 150) String contacto,
        Boolean activo) {
}
