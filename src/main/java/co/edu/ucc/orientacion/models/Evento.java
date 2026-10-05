package co.edu.ucc.orientacion.models;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Evento institucional. Los eventos pasados pasan a CONCLUIDO y nunca se eliminan.
 *
 * @author Gabriela Zabaleta
 * @param id identificador único UUID
 * @param nombre nombre del evento
 * @param descripcion descripción del evento
 * @param categoria categoría del evento
 * @param lugar lugar de realización
 * @param fechaHora fecha y hora del evento
 * @param cupos cupos disponibles
 * @param estado estado: ACTIVO, CONCLUIDO o CANCELADO
 * @param creadoPor administrador autor del evento
 * @param creadoEn fecha de creación
 */
public record Evento(
        UUID id,
        String nombre,
        String descripcion,
        String categoria,
        String lugar,
        LocalDateTime fechaHora,
        Integer cupos,
        String estado,
        UUID creadoPor,
        LocalDateTime creadoEn) {
}
