package co.edu.ucc.orientacion.dto.response;

/**
 * Rol disponible en el sistema.
 *
 * @author Doris Arzuaga
 * @param nombre nombre del rol
 * @param descripcion descripción de los permisos del rol
 */
public record RoleResponse(String nombre, String descripcion) {
}
