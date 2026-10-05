package co.edu.ucc.orientacion.services;

import co.edu.ucc.orientacion.dto.response.PageResponse;
import co.edu.ucc.orientacion.dto.response.RoleResponse;
import co.edu.ucc.orientacion.dto.response.UserResponse;
import co.edu.ucc.orientacion.exceptions.BadRequestException;
import co.edu.ucc.orientacion.exceptions.ForbiddenException;
import co.edu.ucc.orientacion.exceptions.NotFoundException;
import co.edu.ucc.orientacion.models.Usuario;
import co.edu.ucc.orientacion.repositories.AuditRepository;
import co.edu.ucc.orientacion.repositories.UserRepository;
import co.edu.ucc.orientacion.utils.TextUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lógica de negocio de administración de usuarios: búsqueda, activación, cambio de rol y
 * eliminación definitiva, con registro en auditoría.
 *
 * @author Doris Arzuaga
 */
@Service
public class AdminService {

    public static final int PAGE_SIZE = 10;
    public static final String ROLE_STUDENT = "ESTUDIANTE";
    public static final String ROLE_ADMIN = "ADMINISTRADOR";

    private static final String ENTITY_USER = "usuario";

    private final UserRepository userRepository;
    private final AuditRepository auditRepository;
    private final ObjectMapper objectMapper;

    /**
     * Crea el servicio con sus dependencias.
     *
     * @author Doris Arzuaga
     * @param userRepository repositorio de usuarios
     * @param auditRepository repositorio de auditoría
     * @param objectMapper serializador JSON del detalle de auditoría
     */
    public AdminService(UserRepository userRepository, AuditRepository auditRepository, ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.auditRepository = auditRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Lista usuarios de forma paginada, 10 por página, buscando por nombre, correo o programa.
     *
     * @author Doris Arzuaga
     * @param search texto de búsqueda; null para no filtrar; la búsqueda ignora tildes y mayúsculas
     * @param active true para solo cuentas activas, false para solo inactivas, null para todas
     * @param page número de página, iniciando en 1
     * @return página de usuarios
     * @throws BadRequestException cuando la página es menor que 1
     */
    public PageResponse<UserResponse> listUsers(String search, Boolean active, int page) {
        if (page < 1) {
            throw new BadRequestException("La página debe ser mayor o igual a 1");
        }
        String pattern = TextUtils.likePattern(search);
        List<UserResponse> content = userRepository.search(pattern, active, PAGE_SIZE, (page - 1) * PAGE_SIZE).stream()
                .map(UserResponse::from)
                .toList();
        return PageResponse.of(content, page, PAGE_SIZE, userRepository.countSearch(pattern, active));
    }

    /**
     * Activa o desactiva una cuenta. Al desactivarla se invalidan sus sesiones. La acción queda
     * registrada en auditoría.
     *
     * @author Doris Arzuaga
     * @param actorId administrador que ejecuta la acción
     * @param targetId usuario afectado
     * @param activo true para activar, false para desactivar
     * @return usuario actualizado
     * @throws ForbiddenException cuando el administrador intenta actuar sobre su propia cuenta
     * @throws NotFoundException cuando el usuario no existe
     */
    @Transactional
    public UserResponse updateStatus(UUID actorId, UUID targetId, boolean activo) {
        ensureNotSelf(actorId, targetId);
        findUser(targetId);
        userRepository.updateActive(targetId, activo, LocalDateTime.now());
        if (!activo) {
            userRepository.invalidateRefreshTokens(targetId);
        }
        audit(actorId, activo ? "USUARIO_ACTIVADO" : "USUARIO_DESACTIVADO", targetId, Map.of("activo", activo));
        return UserResponse.from(findUser(targetId));
    }

    /**
     * Cambia el rol de un usuario entre ESTUDIANTE y ADMINISTRADOR. La acción queda registrada
     * en auditoría.
     *
     * @author Doris Arzuaga
     * @param actorId administrador que ejecuta la acción
     * @param targetId usuario afectado
     * @param rol nuevo rol
     * @return usuario actualizado
     * @throws ForbiddenException cuando el administrador intenta cambiar su propio rol
     * @throws BadRequestException cuando el rol no es válido
     * @throws NotFoundException cuando el usuario no existe
     */
    @Transactional
    public UserResponse updateRole(UUID actorId, UUID targetId, String rol) {
        ensureNotSelf(actorId, targetId);
        String newRole = TextUtils.normalizeKey(rol);
        if (!ROLE_STUDENT.equals(newRole) && !ROLE_ADMIN.equals(newRole)) {
            throw new BadRequestException("Rol inválido. Valores permitidos: " + ROLE_STUDENT + ", " + ROLE_ADMIN);
        }
        Usuario target = findUser(targetId);
        if (target.rol().equals(newRole)) {
            return UserResponse.from(target);
        }
        userRepository.updateRole(targetId, newRole, LocalDateTime.now());
        audit(actorId, "ROL_CAMBIADO", targetId, Map.of("rolAnterior", target.rol(), "rolNuevo", newRole));
        return UserResponse.from(findUser(targetId));
    }

    /**
     * Elimina definitivamente a un usuario y sus datos personales, en ejercicio del derecho de
     * supresión de la Ley 1581 de 2012. El contenido institucional que creó pasa al administrador
     * que ejecuta la acción. La auditoría no conserva datos personales del usuario eliminado.
     *
     * @author Doris Arzuaga
     * @param actorId administrador que ejecuta la acción
     * @param targetId usuario a eliminar
     * @throws ForbiddenException cuando el administrador intenta eliminar su propia cuenta
     * @throws NotFoundException cuando el usuario no existe
     */
    @Transactional
    public void deleteUser(UUID actorId, UUID targetId) {
        ensureNotSelf(actorId, targetId);
        Usuario target = findUser(targetId);
        userRepository.deletePermanently(targetId, actorId);
        audit(actorId, "USUARIO_ELIMINADO", targetId,
                Map.of("rol", target.rol(), "motivo", "Derecho de supresión Ley 1581 de 2012"));
    }

    /**
     * Lista los roles disponibles en el sistema.
     *
     * @author Doris Arzuaga
     * @return roles con su descripción
     */
    public List<RoleResponse> listRoles() {
        return List.of(
                new RoleResponse(ROLE_STUDENT, "Consulta horarios, servicios, campus, noticias y eventos"),
                new RoleResponse(ROLE_ADMIN, "Administra usuarios y el contenido institucional"));
    }

    private void ensureNotSelf(UUID actorId, UUID targetId) {
        if (actorId.equals(targetId)) {
            throw new ForbiddenException("Un administrador no puede actuar sobre su propia cuenta");
        }
    }

    private Usuario findUser(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
    }

    private void audit(UUID actorId, String accion, UUID entityId, Map<String, Object> detail) {
        try {
            auditRepository.insert(
                    actorId, accion, ENTITY_USER, entityId, objectMapper.writeValueAsString(detail), LocalDateTime.now());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No fue posible serializar el detalle de auditoría", e);
        }
    }
}
