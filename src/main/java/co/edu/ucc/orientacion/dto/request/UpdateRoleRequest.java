package co.edu.ucc.orientacion.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Solicitud administrativa para cambiar el rol de un usuario.
 *
 * @author Doris Arzuaga
 * @param rol nuevo rol: ESTUDIANTE o ADMINISTRADOR
 */
public record UpdateRoleRequest(@NotBlank String rol) {
}
