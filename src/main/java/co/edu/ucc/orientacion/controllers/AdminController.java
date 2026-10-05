package co.edu.ucc.orientacion.controllers;

import co.edu.ucc.orientacion.dto.request.UpdateRoleRequest;
import co.edu.ucc.orientacion.dto.request.UpdateStatusRequest;
import co.edu.ucc.orientacion.dto.response.ApiResponse;
import co.edu.ucc.orientacion.security.CurrentUser;
import co.edu.ucc.orientacion.services.AdminService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Endpoints de administración de usuarios. Todos exigen el rol ADMINISTRADOR.
 *
 * @author Doris Arzuaga
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AdminService adminService;

    /**
     * Crea el controller con el servicio de administración.
     *
     * @author Doris Arzuaga
     * @param adminService servicio de administración
     */
    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    /**
     * Lista usuarios de forma paginada, buscando por nombre, correo o programa.
     *
     * @author Doris Arzuaga
     * @param search texto de búsqueda; opcional; la búsqueda ignora tildes y mayúsculas
     * @param active true para solo cuentas activas, false para solo inactivas; opcional
     * @param page número de página, iniciando en 1
     * @return ResponseEntity con HTTP 200 y la página de usuarios
     * @throws co.edu.ucc.orientacion.exceptions.BadRequestException cuando la página es menor que 1
     */
    @GetMapping("/users")
    public ResponseEntity<ApiResponse> listUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "1") int page) {
        return ResponseEntity.ok(
                ApiResponse.ok(adminService.listUsers(search, active, page), "Usuarios obtenidos correctamente"));
    }

    /**
     * Activa o desactiva la cuenta de un usuario.
     *
     * @author Doris Arzuaga
     * @param authentication autenticación del administrador
     * @param id identificador del usuario afectado
     * @param request nuevo estado de la cuenta
     * @return ResponseEntity con HTTP 200 y el usuario actualizado
     * @throws co.edu.ucc.orientacion.exceptions.ForbiddenException cuando el administrador actúa sobre su propia cuenta
     * @throws co.edu.ucc.orientacion.exceptions.NotFoundException cuando el usuario no existe
     */
    @PutMapping("/users/{id}/status")
    public ResponseEntity<ApiResponse> updateStatus(
            Authentication authentication, @PathVariable UUID id, @Valid @RequestBody UpdateStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                adminService.updateStatus(CurrentUser.id(authentication), id, request.activo()),
                "Estado de la cuenta actualizado correctamente"));
    }

    /**
     * Cambia el rol de un usuario entre ESTUDIANTE y ADMINISTRADOR.
     *
     * @author Doris Arzuaga
     * @param authentication autenticación del administrador
     * @param id identificador del usuario afectado
     * @param request nuevo rol
     * @return ResponseEntity con HTTP 200 y el usuario actualizado
     * @throws co.edu.ucc.orientacion.exceptions.ForbiddenException cuando el administrador actúa sobre su propia cuenta
     * @throws co.edu.ucc.orientacion.exceptions.NotFoundException cuando el usuario no existe
     */
    @PutMapping("/users/{id}/role")
    public ResponseEntity<ApiResponse> updateRole(
            Authentication authentication, @PathVariable UUID id, @Valid @RequestBody UpdateRoleRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                adminService.updateRole(CurrentUser.id(authentication), id, request.rol()),
                "Rol actualizado correctamente"));
    }

    /**
     * Elimina definitivamente a un usuario (derecho de supresión de la Ley 1581 de 2012).
     *
     * @author Doris Arzuaga
     * @param authentication autenticación del administrador
     * @param id identificador del usuario a eliminar
     * @return ResponseEntity con HTTP 200
     * @throws co.edu.ucc.orientacion.exceptions.ForbiddenException cuando el administrador intenta eliminar su propia cuenta
     * @throws co.edu.ucc.orientacion.exceptions.NotFoundException cuando el usuario no existe
     */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<ApiResponse> deleteUser(Authentication authentication, @PathVariable UUID id) {
        adminService.deleteUser(CurrentUser.id(authentication), id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Usuario eliminado definitivamente"));
    }

    /**
     * Lista los roles disponibles en el sistema.
     *
     * @author Doris Arzuaga
     * @return ResponseEntity con HTTP 200 y los roles
     */
    @GetMapping("/roles")
    public ResponseEntity<ApiResponse> listRoles() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.listRoles(), "Roles obtenidos correctamente"));
    }
}
