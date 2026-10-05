package co.edu.ucc.orientacion.controllers;

import co.edu.ucc.orientacion.dto.request.LoginRequest;
import co.edu.ucc.orientacion.dto.request.PasswordRecoveryRequest;
import co.edu.ucc.orientacion.dto.request.PasswordResetRequest;
import co.edu.ucc.orientacion.dto.request.RefreshRequest;
import co.edu.ucc.orientacion.dto.request.RegisterRequest;
import co.edu.ucc.orientacion.dto.request.UpdateProfileRequest;
import co.edu.ucc.orientacion.dto.response.ApiResponse;
import co.edu.ucc.orientacion.security.CurrentUser;
import co.edu.ucc.orientacion.services.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * Endpoints de autenticación y de perfil del usuario.
 *
 * @author Doris Arzuaga
 */
@RestController
@RequestMapping("/api/v1")
public class AuthController {

    private final AuthService authService;

    /**
     * Crea el controller con el servicio de autenticación.
     *
     * @author Doris Arzuaga
     * @param authService servicio de autenticación
     */
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Registra un nuevo estudiante validando el correo institucional y registrando el
     * consentimiento de datos conforme a la Ley 1581 de 2012.
     *
     * @author Doris Arzuaga
     * @param request DTO con los datos de registro del estudiante
     * @return ResponseEntity con HTTP 201 y el ID del usuario creado
     * @throws co.edu.ucc.orientacion.exceptions.ConflictException cuando el correo ya está registrado
     * @throws co.edu.ucc.orientacion.exceptions.UnprocessableEntityException cuando el consentimiento no fue otorgado
     */
    @PostMapping("/auth/register")
    public ResponseEntity<ApiResponse> register(@Valid @RequestBody RegisterRequest request) {
        UUID id = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(Map.of("id", id), "Usuario registrado correctamente"));
    }

    /**
     * Inicia sesión y entrega los tokens de acceso y de refresco.
     *
     * @author Doris Arzuaga
     * @param request credenciales del usuario
     * @return ResponseEntity con HTTP 200 y los tokens emitidos
     * @throws co.edu.ucc.orientacion.exceptions.UnauthorizedException cuando las credenciales son inválidas o la cuenta está bloqueada
     */
    @PostMapping("/auth/login")
    public ResponseEntity<ApiResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.login(request), "Inicio de sesión exitoso"));
    }

    /**
     * Cierra la sesión invalidando los refresh tokens del usuario autenticado.
     *
     * @author Doris Arzuaga
     * @param authentication autenticación de la solicitud
     * @return ResponseEntity con HTTP 200
     */
    @PostMapping("/auth/logout")
    public ResponseEntity<ApiResponse> logout(Authentication authentication) {
        authService.logout(CurrentUser.id(authentication));
        return ResponseEntity.ok(ApiResponse.ok(null, "Sesión cerrada correctamente"));
    }

    /**
     * Solicita la recuperación de contraseña. Siempre responde HTTP 200 sin revelar si el
     * correo existe.
     *
     * @author Doris Arzuaga
     * @param request correo del usuario
     * @return ResponseEntity con HTTP 200 y un mensaje genérico
     */
    @PostMapping("/auth/password-recovery")
    public ResponseEntity<ApiResponse> passwordRecovery(@Valid @RequestBody PasswordRecoveryRequest request) {
        authService.requestPasswordRecovery(request);
        return ResponseEntity.ok(ApiResponse.ok(null,
                "Si el correo está registrado, recibirá instrucciones para restablecer su contraseña"));
    }

    /**
     * Restablece la contraseña usando un token de recuperación de un solo uso.
     *
     * @author Doris Arzuaga
     * @param request token de recuperación y nueva contraseña
     * @return ResponseEntity con HTTP 200
     * @throws co.edu.ucc.orientacion.exceptions.BadRequestException cuando el token es inválido, expiró o ya fue usado
     */
    @PostMapping("/auth/password-reset")
    public ResponseEntity<ApiResponse> passwordReset(@Valid @RequestBody PasswordResetRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.ok(null, "Contraseña actualizada correctamente"));
    }

    /**
     * Genera un nuevo access token a partir de un refresh token válido.
     *
     * @author Doris Arzuaga
     * @param request refresh token emitido durante el inicio de sesión
     * @return ResponseEntity con HTTP 200 y el nuevo access token
     * @throws co.edu.ucc.orientacion.exceptions.UnauthorizedException cuando el refresh token no es válido
     */
    @PostMapping("/auth/refresh")
    public ResponseEntity<ApiResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.refresh(request), "Token renovado correctamente"));
    }

    /**
     * Obtiene el perfil del usuario autenticado.
     *
     * @author Doris Arzuaga
     * @param authentication autenticación de la solicitud
     * @return ResponseEntity con HTTP 200 y los datos del perfil
     */
    @GetMapping("/users/profile")
    public ResponseEntity<ApiResponse> getProfile(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(
                authService.getProfile(CurrentUser.id(authentication)), "Perfil obtenido correctamente"));
    }

    /**
     * Actualiza nombre, apellido, programa y teléfono del usuario autenticado. El correo y el
     * rol son de solo lectura.
     *
     * @author Doris Arzuaga
     * @param authentication autenticación de la solicitud
     * @param request nuevos datos del perfil
     * @return ResponseEntity con HTTP 200 y el perfil actualizado
     */
    @PutMapping("/users/profile")
    public ResponseEntity<ApiResponse> updateProfile(
            Authentication authentication, @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                authService.updateProfile(CurrentUser.id(authentication), request), "Perfil actualizado correctamente"));
    }
}
