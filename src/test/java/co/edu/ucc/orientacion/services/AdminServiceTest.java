package co.edu.ucc.orientacion.services;

import co.edu.ucc.orientacion.dto.response.PageResponse;
import co.edu.ucc.orientacion.dto.response.UserResponse;
import co.edu.ucc.orientacion.exceptions.BadRequestException;
import co.edu.ucc.orientacion.exceptions.ForbiddenException;
import co.edu.ucc.orientacion.exceptions.NotFoundException;
import co.edu.ucc.orientacion.models.Usuario;
import co.edu.ucc.orientacion.repositories.AuditRepository;
import co.edu.ucc.orientacion.repositories.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Pruebas de la lógica de administración de usuarios y de auditoría.
 *
 * @author Doris Arzuaga
 */
@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    private final UUID actorId = UUID.randomUUID();
    private final UUID targetId = UUID.randomUUID();

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditRepository auditRepository;

    private AdminService adminService;

    @BeforeEach
    void setUp() {
        adminService = new AdminService(userRepository, auditRepository, new ObjectMapper());
    }

    private Usuario usuario(UUID id, String rol, boolean activo) {
        return new Usuario(id, "Luis", "Gómez", "luis@campusucc.edu.co", "hash", "Derecho", null, rol, activo, true,
                LocalDateTime.now(), 0, null, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    @DisplayName("Listar usuarios con página menor que 1 responde 400")
    void listUsersWithInvalidPageFails() {
        assertThrows(BadRequestException.class, () -> adminService.listUsers(null, null, 0));
    }

    @Test
    @DisplayName("Listar usuarios pagina de a 10 y busca con patrón insensible a mayúsculas")
    void listUsersPaginates() {
        when(userRepository.search("%luis%", null, 10, 20)).thenReturn(List.of(usuario(targetId, "ESTUDIANTE", true)));
        when(userRepository.countSearch("%luis%", null)).thenReturn(21L);

        PageResponse<UserResponse> page = adminService.listUsers("luis", null, 3);

        assertEquals(3, page.page());
        assertEquals(3, page.totalPages());
        assertEquals(1, page.content().size());
    }

    @Test
    @DisplayName("La respuesta de usuario no expone el hash de la contraseña")
    void userResponseHidesPasswordHash() throws Exception {
        String json = new ObjectMapper().findAndRegisterModules()
                .writeValueAsString(UserResponse.from(usuario(targetId, "ESTUDIANTE", true)));

        assertFalse(json.toLowerCase().contains("hash"));
        assertFalse(json.toLowerCase().contains("contrasena"));
    }

    @Test
    @DisplayName("Un administrador no puede desactivar su propia cuenta")
    void adminCannotChangeOwnStatus() {
        assertThrows(ForbiddenException.class, () -> adminService.updateStatus(actorId, actorId, false));

        verifyNoInteractions(userRepository, auditRepository);
    }

    @Test
    @DisplayName("Un administrador no puede cambiar su propio rol")
    void adminCannotChangeOwnRole() {
        assertThrows(ForbiddenException.class, () -> adminService.updateRole(actorId, actorId, "ESTUDIANTE"));

        verifyNoInteractions(userRepository, auditRepository);
    }

    @Test
    @DisplayName("Un administrador no puede eliminar su propia cuenta")
    void adminCannotDeleteOwnAccount() {
        assertThrows(ForbiddenException.class, () -> adminService.deleteUser(actorId, actorId));

        verifyNoInteractions(userRepository, auditRepository);
    }

    @Test
    @DisplayName("Desactivar una cuenta invalida sus sesiones y queda auditado")
    void deactivatingInvalidatesSessionsAndAudits() {
        when(userRepository.findById(targetId)).thenReturn(Optional.of(usuario(targetId, "ESTUDIANTE", false)));

        adminService.updateStatus(actorId, targetId, false);

        verify(userRepository).updateActive(eq(targetId), eq(false), any(LocalDateTime.class));
        verify(userRepository).invalidateRefreshTokens(targetId);
        verify(auditRepository).insert(eq(actorId), eq("USUARIO_DESACTIVADO"), eq("usuario"), eq(targetId), anyString(),
                any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Activar una cuenta queda auditado y no invalida sesiones")
    void activatingAuditsWithoutInvalidatingSessions() {
        when(userRepository.findById(targetId)).thenReturn(Optional.of(usuario(targetId, "ESTUDIANTE", true)));

        adminService.updateStatus(actorId, targetId, true);

        verify(userRepository, never()).invalidateRefreshTokens(any());
        verify(auditRepository).insert(eq(actorId), eq("USUARIO_ACTIVADO"), eq("usuario"), eq(targetId), anyString(),
                any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Actuar sobre un usuario inexistente responde 404")
    void unknownUserFails() {
        when(userRepository.findById(targetId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> adminService.updateStatus(actorId, targetId, true));
        assertThrows(NotFoundException.class, () -> adminService.updateRole(actorId, targetId, "ADMINISTRADOR"));
        assertThrows(NotFoundException.class, () -> adminService.deleteUser(actorId, targetId));
    }

    @Test
    @DisplayName("Cambiar a un rol inexistente responde 400")
    void invalidRoleFails() {
        assertThrows(BadRequestException.class, () -> adminService.updateRole(actorId, targetId, "SUPERUSUARIO"));
    }

    @Test
    @DisplayName("Cambiar el rol actualiza al usuario y registra el rol anterior y el nuevo en auditoría")
    void changingRoleAudits() {
        when(userRepository.findById(targetId)).thenReturn(Optional.of(usuario(targetId, "ESTUDIANTE", true)));
        ArgumentCaptor<String> detail = ArgumentCaptor.forClass(String.class);

        adminService.updateRole(actorId, targetId, "administrador");

        verify(userRepository).updateRole(eq(targetId), eq("ADMINISTRADOR"), any(LocalDateTime.class));
        verify(auditRepository).insert(eq(actorId), eq("ROL_CAMBIADO"), eq("usuario"), eq(targetId), detail.capture(),
                any(LocalDateTime.class));
        assertTrue(detail.getValue().contains("\"rolAnterior\":\"ESTUDIANTE\""));
        assertTrue(detail.getValue().contains("\"rolNuevo\":\"ADMINISTRADOR\""));
    }

    @Test
    @DisplayName("Asignar el mismo rol no modifica ni audita")
    void sameRoleIsNoOp() {
        when(userRepository.findById(targetId)).thenReturn(Optional.of(usuario(targetId, "ESTUDIANTE", true)));

        adminService.updateRole(actorId, targetId, "ESTUDIANTE");

        verify(userRepository, never()).updateRole(any(), any(), any());
        verifyNoInteractions(auditRepository);
    }

    @Test
    @DisplayName("La eliminación definitiva reasigna el contenido al administrador y audita sin datos personales")
    void deletingUserIsAuditedWithoutPersonalData() {
        when(userRepository.findById(targetId)).thenReturn(Optional.of(usuario(targetId, "ESTUDIANTE", true)));
        ArgumentCaptor<String> detail = ArgumentCaptor.forClass(String.class);

        adminService.deleteUser(actorId, targetId);

        verify(userRepository).deletePermanently(targetId, actorId);
        verify(auditRepository).insert(eq(actorId), eq("USUARIO_ELIMINADO"), eq("usuario"), eq(targetId), detail.capture(),
                any(LocalDateTime.class));
        assertFalse(detail.getValue().contains("luis@campusucc.edu.co"));
        assertFalse(detail.getValue().contains("Luis"));
    }

    @Test
    @DisplayName("Los roles disponibles son ESTUDIANTE y ADMINISTRADOR")
    void rolesAreListed() {
        assertEquals(List.of("ESTUDIANTE", "ADMINISTRADOR"),
                adminService.listRoles().stream().map(role -> role.nombre()).toList());
    }
}
