package co.edu.ucc.orientacion.models;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;
import java.util.UUID;

public record Usuario(
        UUID id,
        String nombre,
        String apellido,
        String correo,
        @JsonIgnore String contrasenaHash,
        String programaAcademico,
        String telefono,
        String rol,
        boolean activo,
        boolean consentimientoDatos,
        LocalDateTime consentimientoFecha,
        int intentosFallidos,
        LocalDateTime bloqueadoHasta,
        LocalDateTime creadoEn,
        LocalDateTime actualizadoEn) {
}
