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

@RestController
@RequestMapping("/api/v1")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/auth/register")
    public ResponseEntity<ApiResponse> register(@Valid @RequestBody RegisterRequest request) {
        UUID id = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(Map.of("id", id), "Usuario registrado correctamente"));
    }

    @PostMapping("/auth/login")
    public ResponseEntity<ApiResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.login(request), "Inicio de sesión exitoso"));
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<ApiResponse> logout(Authentication authentication) {
        authService.logout(CurrentUser.id(authentication));
        return ResponseEntity.ok(ApiResponse.ok(null, "Sesión cerrada correctamente"));
    }

    @PostMapping("/auth/password-recovery")
    public ResponseEntity<ApiResponse> passwordRecovery(@Valid @RequestBody PasswordRecoveryRequest request) {
        authService.requestPasswordRecovery(request);
        return ResponseEntity.ok(ApiResponse.ok(null,
                "Si el correo está registrado, recibirá instrucciones para restablecer su contraseña"));
    }

    @PostMapping("/auth/password-reset")
    public ResponseEntity<ApiResponse> passwordReset(@Valid @RequestBody PasswordResetRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.ok(null, "Contraseña actualizada correctamente"));
    }

    @PostMapping("/auth/refresh")
    public ResponseEntity<ApiResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.refresh(request), "Token renovado correctamente"));
    }

    @GetMapping("/users/profile")
    public ResponseEntity<ApiResponse> getProfile(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(
                authService.getProfile(CurrentUser.id(authentication)), "Perfil obtenido correctamente"));
    }

    @PutMapping("/users/profile")
    public ResponseEntity<ApiResponse> updateProfile(
            Authentication authentication, @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                authService.updateProfile(CurrentUser.id(authentication), request), "Perfil actualizado correctamente"));
    }
}
