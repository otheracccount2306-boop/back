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

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse> listUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "1") int page) {
        return ResponseEntity.ok(
                ApiResponse.ok(adminService.listUsers(search, active, page), "Usuarios obtenidos correctamente"));
    }

    @PutMapping("/users/{id}/status")
    public ResponseEntity<ApiResponse> updateStatus(
            Authentication authentication, @PathVariable UUID id, @Valid @RequestBody UpdateStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                adminService.updateStatus(CurrentUser.id(authentication), id, request.activo()),
                "Estado de la cuenta actualizado correctamente"));
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<ApiResponse> updateRole(
            Authentication authentication, @PathVariable UUID id, @Valid @RequestBody UpdateRoleRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                adminService.updateRole(CurrentUser.id(authentication), id, request.rol()),
                "Rol actualizado correctamente"));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<ApiResponse> deleteUser(Authentication authentication, @PathVariable UUID id) {
        adminService.deleteUser(CurrentUser.id(authentication), id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Usuario eliminado definitivamente"));
    }

    @GetMapping("/roles")
    public ResponseEntity<ApiResponse> listRoles() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.listRoles(), "Roles obtenidos correctamente"));
    }
}
