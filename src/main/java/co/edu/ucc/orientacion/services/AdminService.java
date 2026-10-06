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

@Service
public class AdminService {

    public static final int PAGE_SIZE = 10;
    public static final String ROLE_STUDENT = "ESTUDIANTE";
    public static final String ROLE_ADMIN = "ADMINISTRADOR";

    private static final String ENTITY_USER = "usuario";

    private final UserRepository userRepository;
    private final AuditRepository auditRepository;
    private final ObjectMapper objectMapper;

    public AdminService(UserRepository userRepository, AuditRepository auditRepository, ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.auditRepository = auditRepository;
        this.objectMapper = objectMapper;
    }

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

    @Transactional
    public void deleteUser(UUID actorId, UUID targetId) {
        ensureNotSelf(actorId, targetId);
        Usuario target = findUser(targetId);
        userRepository.deletePermanently(targetId, actorId);
        audit(actorId, "USUARIO_ELIMINADO", targetId,
                Map.of("rol", target.rol(), "motivo", "Derecho de supresión Ley 1581 de 2012"));
    }

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
