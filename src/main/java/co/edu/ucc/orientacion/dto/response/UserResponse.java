package co.edu.ucc.orientacion.dto.response;

import co.edu.ucc.orientacion.models.Usuario;

import java.time.LocalDateTime;
import java.util.UUID;

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
