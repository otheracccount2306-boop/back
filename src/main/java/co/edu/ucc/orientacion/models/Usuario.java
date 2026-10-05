package co.edu.ucc.orientacion.models;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Representa un usuario registrado en el sistema (estudiante o administrador).
 *
 * @author Doris Arzuaga
 * @param id identificador único UUID
 * @param nombre nombres del usuario
 * @param apellido apellidos del usuario
 * @param correo correo institucional, único en el sistema
 * @param contrasenaHash hash BCrypt de la contraseña, nunca se serializa
 * @param programaAcademico programa académico cursado
 * @param telefono teléfono de contacto
 * @param rol rol del usuario: ESTUDIANTE o ADMINISTRADOR
 * @param activo indica si la cuenta está habilitada
 * @param consentimientoDatos indica si otorgó el tratamiento de datos personales
 * @param consentimientoFecha fecha en que se otorgó el consentimiento
 * @param intentosFallidos contador de intentos de inicio de sesión fallidos
 * @param bloqueadoHasta fecha hasta la cual la cuenta permanece bloqueada
 * @param creadoEn fecha de creación
 * @param actualizadoEn fecha de la última actualización
 */
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
