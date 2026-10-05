package co.edu.ucc.orientacion.services;

import co.edu.ucc.orientacion.config.JwtConfig;
import co.edu.ucc.orientacion.dto.request.LoginRequest;
import co.edu.ucc.orientacion.dto.request.PasswordRecoveryRequest;
import co.edu.ucc.orientacion.dto.request.PasswordResetRequest;
import co.edu.ucc.orientacion.dto.request.RefreshRequest;
import co.edu.ucc.orientacion.dto.request.RegisterRequest;
import co.edu.ucc.orientacion.dto.response.AuthResponse;
import co.edu.ucc.orientacion.exceptions.BadRequestException;
import co.edu.ucc.orientacion.exceptions.ConflictException;
import co.edu.ucc.orientacion.exceptions.UnauthorizedException;
import co.edu.ucc.orientacion.exceptions.UnprocessableEntityException;
import co.edu.ucc.orientacion.models.Usuario;
import co.edu.ucc.orientacion.repositories.UserRepository;
import co.edu.ucc.orientacion.security.JwtUtils;
import co.edu.ucc.orientacion.utils.HashUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Pruebas de la lógica de autenticación, registro y recuperación de contraseña.
 *
 * @author Doris Arzuaga
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String PASSWORD = "Clave1234";

    @Mock
    private UserRepository userRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private final JwtUtils jwtUtils =
            new JwtUtils(new JwtConfig("ucc_orientacion_jwt_secret_2026_spring", 86_400_000L, 604_800_000L));

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtUtils, "campusucc.edu.co, ucc.edu.co");
    }

    private RegisterRequest registerRequest(String correo, boolean consentimiento) {
        return new RegisterRequest("Ana", "Pérez", correo, PASSWORD, "Ingeniería de Software", "3001234567", consentimiento);
    }

    private Usuario usuario(UUID id, boolean activo, LocalDateTime bloqueadoHasta) {
        return new Usuario(id, "Ana", "Pérez", "ana@campusucc.edu.co", passwordEncoder.encode(PASSWORD), null, null,
                "ESTUDIANTE", activo, true, LocalDateTime.now(), 0, bloqueadoHasta, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    @DisplayName("El registro sin consentimiento de datos responde 422 y no toca la base de datos")
    void registerWithoutConsentIsRejected() {
        assertThrows(UnprocessableEntityException.class,
                () -> authService.register(registerRequest("ana@campusucc.edu.co", false)));

        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("El registro con un correo fuera de los dominios institucionales responde 400")
    void registerWithNonInstitutionalEmailIsRejected() {
        assertThrows(BadRequestException.class,
                () -> authService.register(registerRequest("ana@gmail.com", true)));
    }

    @Test
    @DisplayName("El registro con un correo ya existente responde 409")
    void registerWithExistingEmailIsRejected() {
        when(userRepository.existsByCorreo("ana@campusucc.edu.co")).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> authService.register(registerRequest("ANA@campusucc.edu.co", true)));
    }

    @Test
    @DisplayName("El registro guarda el correo en minúsculas y la contraseña cifrada con BCrypt")
    void registerStoresBcryptHash() {
        UUID id = UUID.randomUUID();
        when(userRepository.existsByCorreo("ana@campusucc.edu.co")).thenReturn(false);
        when(userRepository.create(any(), any(), any(), any(), any(), any(), any())).thenReturn(id);
        ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);

        UUID created = authService.register(registerRequest("Ana@CampusUCC.edu.co", true));

        assertEquals(id, created);
        verify(userRepository).create(eq("Ana"), eq("Pérez"), eq("ana@campusucc.edu.co"), hash.capture(),
                eq("Ingeniería de Software"), eq("3001234567"), any(LocalDateTime.class));
        assertNotEquals(PASSWORD, hash.getValue());
        assertTrue(passwordEncoder.matches(PASSWORD, hash.getValue()));
    }

    @Test
    @DisplayName("El login con un correo inexistente responde 401")
    void loginWithUnknownEmailFails() {
        when(userRepository.findByCorreo("nadie@campusucc.edu.co")).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class,
                () -> authService.login(new LoginRequest("nadie@campusucc.edu.co", PASSWORD)));
    }

    @Test
    @DisplayName("El login con contraseña incorrecta registra el intento fallido y responde 401")
    void loginWithWrongPasswordRegistersFailure() {
        UUID id = UUID.randomUUID();
        when(userRepository.findByCorreo("ana@campusucc.edu.co")).thenReturn(Optional.of(usuario(id, true, null)));

        UnauthorizedException error = assertThrows(UnauthorizedException.class,
                () -> authService.login(new LoginRequest("ana@campusucc.edu.co", "incorrecta1")));

        assertEquals("Credenciales inválidas", error.getMessage());
        verify(userRepository).registerFailedLogin(eq(id), eq(5), any(LocalDateTime.class));
        verify(userRepository, never()).saveRefreshToken(any(), any(), any());
    }

    @Test
    @DisplayName("El login de una cuenta bloqueada responde 401 sin comprobar la contraseña ni sumar intentos")
    void loginWithBlockedAccountFails() {
        when(userRepository.findByCorreo("ana@campusucc.edu.co"))
                .thenReturn(Optional.of(usuario(UUID.randomUUID(), true, LocalDateTime.now().plusMinutes(10))));

        assertThrows(UnauthorizedException.class,
                () -> authService.login(new LoginRequest("ana@campusucc.edu.co", PASSWORD)));

        verify(userRepository, never()).registerFailedLogin(any(), anyInt(), any());
    }

    @Test
    @DisplayName("El login de una cuenta cuyo bloqueo ya venció es exitoso")
    void loginAfterLockExpiredSucceeds() {
        UUID id = UUID.randomUUID();
        when(userRepository.findByCorreo("ana@campusucc.edu.co"))
                .thenReturn(Optional.of(usuario(id, true, LocalDateTime.now().minusMinutes(1))));

        AuthResponse response = authService.login(new LoginRequest("ana@campusucc.edu.co", PASSWORD));

        assertEquals("Bearer", response.tokenType());
        verify(userRepository).resetLoginState(id);
    }

    @Test
    @DisplayName("El login de una cuenta desactivada responde 401")
    void loginWithInactiveAccountFails() {
        when(userRepository.findByCorreo("ana@campusucc.edu.co"))
                .thenReturn(Optional.of(usuario(UUID.randomUUID(), false, null)));

        assertThrows(UnauthorizedException.class,
                () -> authService.login(new LoginRequest("ana@campusucc.edu.co", PASSWORD)));
    }

    @Test
    @DisplayName("El login exitoso reinicia intentos y guarda solo el hash del refresh token")
    void loginSucceedsAndStoresRefreshTokenHash() {
        UUID id = UUID.randomUUID();
        when(userRepository.findByCorreo("ana@campusucc.edu.co")).thenReturn(Optional.of(usuario(id, true, null)));
        ArgumentCaptor<String> storedHash = ArgumentCaptor.forClass(String.class);

        AuthResponse response = authService.login(new LoginRequest("Ana@campusucc.edu.co", PASSWORD));

        verify(userRepository).resetLoginState(id);
        verify(userRepository).saveRefreshToken(eq(id), storedHash.capture(), any(LocalDateTime.class));
        assertEquals(HashUtils.sha256Hex(response.refreshToken()), storedHash.getValue());
        assertTrue(jwtUtils.parseAccessToken(response.accessToken()).isPresent());
        assertTrue(jwtUtils.parseRefreshToken(response.refreshToken()).isPresent());
        assertEquals(86_400L, response.expiresIn());
    }

    @Test
    @DisplayName("El refresh con un token que no es un JWT válido responde 401")
    void refreshWithInvalidTokenFails() {
        assertThrows(UnauthorizedException.class, () -> authService.refresh(new RefreshRequest("invalido")));
    }

    @Test
    @DisplayName("El refresh con un access token responde 401")
    void refreshWithAccessTokenFails() {
        String access = jwtUtils.generateAccessToken(UUID.randomUUID(), "ESTUDIANTE");

        assertThrows(UnauthorizedException.class, () -> authService.refresh(new RefreshRequest(access)));
    }

    @Test
    @DisplayName("El refresh con un token invalidado en base de datos responde 401")
    void refreshWithRevokedTokenFails() {
        String refresh = jwtUtils.generateRefreshToken(UUID.randomUUID());
        when(userRepository.findUserIdByValidRefreshToken(eq(HashUtils.sha256Hex(refresh)), any()))
                .thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class, () -> authService.refresh(new RefreshRequest(refresh)));
    }

    @Test
    @DisplayName("El refresh válido entrega un nuevo access token y no rota el refresh token")
    void refreshSucceedsWithoutRotation() {
        UUID id = UUID.randomUUID();
        String refresh = jwtUtils.generateRefreshToken(id);
        when(userRepository.findUserIdByValidRefreshToken(eq(HashUtils.sha256Hex(refresh)), any()))
                .thenReturn(Optional.of(id));
        when(userRepository.findById(id)).thenReturn(Optional.of(usuario(id, true, null)));

        AuthResponse response = authService.refresh(new RefreshRequest(refresh));

        assertEquals(refresh, response.refreshToken());
        assertTrue(jwtUtils.parseAccessToken(response.accessToken()).isPresent());
    }

    @Test
    @DisplayName("El logout invalida los refresh tokens del usuario")
    void logoutInvalidatesRefreshTokens() {
        UUID id = UUID.randomUUID();

        authService.logout(id);

        verify(userRepository).invalidateRefreshTokens(id);
    }

    @Test
    @DisplayName("La recuperación con un correo inexistente no falla y no crea token")
    void recoveryWithUnknownEmailIsSilent() {
        when(userRepository.findByCorreo("nadie@campusucc.edu.co")).thenReturn(Optional.empty());

        authService.requestPasswordRecovery(new PasswordRecoveryRequest("nadie@campusucc.edu.co"));

        verify(userRepository, never()).createRecoveryToken(any(), any(), any());
    }

    @Test
    @DisplayName("La recuperación con un correo existente guarda solo el hash SHA-256 del token con 30 minutos de vigencia")
    void recoveryStoresTokenHash() {
        UUID id = UUID.randomUUID();
        when(userRepository.findByCorreo("ana@campusucc.edu.co")).thenReturn(Optional.of(usuario(id, true, null)));
        ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<LocalDateTime> expiresAt = ArgumentCaptor.forClass(LocalDateTime.class);

        authService.requestPasswordRecovery(new PasswordRecoveryRequest("ana@campusucc.edu.co"));

        verify(userRepository).invalidateRecoveryTokens(id);
        verify(userRepository).createRecoveryToken(eq(id), hash.capture(), expiresAt.capture());
        assertEquals(64, hash.getValue().length());
        assertTrue(expiresAt.getValue().isAfter(LocalDateTime.now().plusMinutes(29)));
        assertTrue(expiresAt.getValue().isBefore(LocalDateTime.now().plusMinutes(31)));
    }

    @Test
    @DisplayName("El restablecimiento con un token inexistente, expirado o usado responde 400")
    void resetWithInvalidTokenFails() {
        when(userRepository.findValidRecoveryToken(any(), any())).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class,
                () -> authService.resetPassword(new PasswordResetRequest("token", "NuevaClave123")));

        verify(userRepository, never()).updatePassword(any(), any(), any());
    }
}
