package co.edu.ucc.orientacion.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;
import java.util.List;

/**
 * Datos de una clase que el estudiante registra en su horario. No incluye el dueño: siempre es
 * el usuario autenticado.
 *
 * @author Diego Luna
 * @param nombre nombre de la clase
 * @param docente nombre del docente; opcional
 * @param aula aula, por código o nombre del espacio; opcional
 * @param dias días de clase, por ejemplo LUNES y MIERCOLES
 * @param horaInicio hora de inicio
 * @param horaFin hora de finalización
 */
public record PersonalClassRequest(
        @NotBlank @Size(max = 150) String nombre,
        @Size(max = 150) String docente,
        @Size(max = 50) String aula,
        @NotEmpty @Size(max = 7) List<@NotBlank String> dias,
        @NotNull LocalTime horaInicio,
        @NotNull LocalTime horaFin) {
}
