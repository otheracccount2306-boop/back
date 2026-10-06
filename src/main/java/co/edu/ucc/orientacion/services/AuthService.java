package co.edu.ucc.orientacion.services;

import co.edu.ucc.orientacion.dto.request.LoginRequest;
import co.edu.ucc.orientacion.dto.request.PasswordRecoveryRequest;
import co.edu.ucc.orientacion.dto.request.PasswordResetRequest;
import co.edu.ucc.orientacion.dto.request.RefreshRequest;
import co.edu.ucc.orientacion.dto.request.RegisterRequest;
import co.edu.ucc.orientacion.dto.request.UpdateProfileRequest;
import co.edu.ucc.orientacion.dto.response.AuthResponse;
import co.edu.ucc.orientacion.dto.response.UserResponse;
import co.edu.ucc.orientacion.exceptions.BadRequestException;
import co.edu.ucc.orientacion.exceptions.ConflictException;
import co.edu.ucc.orientacion.exceptions.NotFoundException;
import co.edu.ucc.orientacion.exceptions.UnauthorizedException;
import co.edu.ucc.orientacion.exceptions.UnprocessableEntityException;
import co.edu.ucc.orientacion.models.TokenRecuperacion;
import co.edu.ucc.orientacion.models.Usuario;
import co.edu.ucc.orientacion.repositories.UserRepository;
import co.edu.ucc.orientacion.security.JwtUtils;
import co.edu.ucc.orientacion.utils.HashUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {

    public static final int MAX_FAILED_ATTEMPTS = 5;
    public static final int LOCK_MINUTES = 15;
    public static final int RECOVERY_TOKEN_MINUTES = 30;

    private static final Logger LOG = LoggerFactory.getLogger(AuthService.class);
    private static final String INVALID_CREDENTIALS = "Credenciales inválidas";
    private static final String INVALID_REFRESH_TOKEN = "Refresh token inválido o expirado";
    private static final String TOKEN_TYPE = "Bearer";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final List<String> institutionalDomains;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtUtils jwtUtils,
            @Value("${app.institutional-email-domains:}") String institutionalDomains) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.institutionalDomains = Arrays.stream(institutionalDomains.split(","))
                .map(domain -> domain.trim().toLowerCase(Locale.ROOT))
                .filter(domain -> !domain.isEmpty())
                .toList();
    }

    public UUID register(RegisterRequest request) {
        if (!request.consentimientoDatos()) {
            throw new UnprocessableEntityException(
                    "Debe aceptar el tratamiento de datos personales conforme a la Ley 1581 de 2012");
        }
        String correo = normalizeEmail(request.correo());
        validateInstitutionalDomain(correo);
        if (userRepository.existsByCorreo(correo)) {
            throw new ConflictException("El correo ya está registrado");
        }
        try {
            return userRepository.create(
                    request.nombre().trim(),
                    request.apellido().trim(),
                    correo,
                    passwordEncoder.encode(request.contrasena()),
                    blankToNull(request.programaAcademico()),
                    blankToNull(request.telefono()),
                    LocalDateTime.now());
        } catch (DuplicateKeyException e) {
            throw new ConflictException("El correo ya está registrado");
        }
    }

    public AuthResponse login(LoginRequest request) {
        Usuario usuario = userRepository.findByCorreo(normalizeEmail(request.correo()))
                .orElseThrow(() -> new UnauthorizedException(INVALID_CREDENTIALS));
        LocalDateTime now = LocalDateTime.now();
        boolean blocked = usuario.bloqueadoHasta() != null && usuario.bloqueadoHasta().isAfter(now);
        if (!usuario.activo() || blocked) {
            throw new UnauthorizedException(INVALID_CREDENTIALS);
        }
        if (!passwordEncoder.matches(request.contrasena(), usuario.contrasenaHash())) {
            userRepository.registerFailedLogin(usuario.id(), MAX_FAILED_ATTEMPTS, now.plusMinutes(LOCK_MINUTES));
            throw new UnauthorizedException(INVALID_CREDENTIALS);
        }
        userRepository.resetLoginState(usuario.id());
        String refreshToken = jwtUtils.generateRefreshToken(usuario.id());
        userRepository.saveRefreshToken(usuario.id(), HashUtils.sha256Hex(refreshToken), jwtUtils.refreshExpiresAt());
        return buildAuthResponse(usuario, refreshToken);
    }

    public AuthResponse refresh(RefreshRequest request) {
        String refreshToken = request.refreshToken();
        String subject = jwtUtils.parseRefreshToken(refreshToken)
                .orElseThrow(() -> new UnauthorizedException(INVALID_REFRESH_TOKEN))
                .getSubject();
        UUID userId = userRepository
                .findUserIdByValidRefreshToken(HashUtils.sha256Hex(refreshToken), LocalDateTime.now())
                .filter(id -> id.toString().equals(subject))
                .orElseThrow(() -> new UnauthorizedException(INVALID_REFRESH_TOKEN));
        Usuario usuario = userRepository.findById(userId)
                .filter(Usuario::activo)
                .orElseThrow(() -> new UnauthorizedException(INVALID_REFRESH_TOKEN));
        return buildAuthResponse(usuario, refreshToken);
    }

    public void logout(UUID userId) {
        userRepository.invalidateRefreshTokens(userId);
    }

    @Transactional
    public void requestPasswordRecovery(PasswordRecoveryRequest request) {
        userRepository.findByCorreo(normalizeEmail(request.correo()))
                .filter(Usuario::activo)
                .ifPresent(usuario -> {
                    userRepository.invalidateRecoveryTokens(usuario.id());
                    String token = HashUtils.generateToken();
                    userRepository.createRecoveryToken(
                            usuario.id(),
                            HashUtils.sha256Hex(token),
                            LocalDateTime.now().plusMinutes(RECOVERY_TOKEN_MINUTES));
                    LOG.info("Token de recuperación de contraseña para {}: {}", usuario.correo(), token);
                });
    }

    @Transactional
    public void resetPassword(PasswordResetRequest request) {
        LocalDateTime now = LocalDateTime.now();
        TokenRecuperacion token = userRepository
                .findValidRecoveryToken(HashUtils.sha256Hex(request.token()), now)
                .orElseThrow(() -> new BadRequestException("Token inválido o expirado"));
        if (!userRepository.markRecoveryTokenUsed(token.id())) {
            throw new BadRequestException("Token inválido o expirado");
        }
        userRepository.updatePassword(token.usuarioId(), passwordEncoder.encode(request.nuevaContrasena()), now);
        userRepository.resetLoginState(token.usuarioId());
        userRepository.invalidateRefreshTokens(token.usuarioId());
    }

    public UserResponse getProfile(UUID userId) {
        return UserResponse.from(findUser(userId));
    }

    @Transactional
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        findUser(userId);
        userRepository.updateProfile(
                userId,
                request.nombre().trim(),
                request.apellido().trim(),
                blankToNull(request.programaAcademico()),
                blankToNull(request.telefono()),
                LocalDateTime.now());
        return UserResponse.from(findUser(userId));
    }

    private Usuario findUser(UUID userId) {
        return userRepository.findById(userId).orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
    }

    private AuthResponse buildAuthResponse(Usuario usuario, String refreshToken) {
        return new AuthResponse(
                jwtUtils.generateAccessToken(usuario.id(), usuario.rol()),
                refreshToken,
                TOKEN_TYPE,
                jwtUtils.getExpirationSeconds(),
                UserResponse.from(usuario));
    }

    private void validateInstitutionalDomain(String correo) {
        if (institutionalDomains.isEmpty()) {
            return;
        }
        String domain = correo.substring(correo.indexOf('@') + 1);
        if (!institutionalDomains.contains(domain)) {
            throw new BadRequestException("Debe usar su correo institucional");
        }
    }

    private String normalizeEmail(String correo) {
        return correo.trim().toLowerCase(Locale.ROOT);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
