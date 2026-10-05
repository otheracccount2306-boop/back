package co.edu.ucc.orientacion.dto.response;

import co.edu.ucc.orientacion.models.Usuario;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Vista pública de un usuario, sin datos sensibles como el hash de la contraseña.
 *
 * @author Doris Arzuaga
 * @param id identificador único UUID
 * @param nombre nombres del usuario
 * @param apellido apellidos del usuario
 * @param correo correo institucional
 * @param programaAcademico programa académico cursado
 * @param telefono teléfono de contacto
 * @param rol rol del usuario
 * @param activo indica si la cuenta está habilitada
 * @param creadoEn fecha de creación
 */
public record UserResponse(
        UUID id,
        String nombre,
        String apellido,
        String correo,
        String programaAcademico,
        String telefono,
        String rol,
        boolean activo,
        LocalDateTime creadoEn) {

    /**
     * Construye la vista pública a partir de la entidad de usuario.
     *
     * @author Doris Arzuaga
     * @param usuario entidad de usuario
     * @return DTO sin datos sensibles
     */
    public static UserResponse from(Usuario usuario) {
        return new UserResponse(
                usuario.id(),
                usuario.nombre(),
                usuario.apellido(),
                usuario.correo(),
                usuario.programaAcademico(),
                usuario.telefono(),
                usuario.rol(),
                usuario.activo(),
                usuario.creadoEn());
    }
}
