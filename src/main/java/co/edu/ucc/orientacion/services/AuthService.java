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

/**
 * Lógica de negocio de autenticación, recuperación de contraseña y perfil de usuario.
 *
 * @author Doris Arzuaga
 */
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

    /**
     * Crea el servicio con sus dependencias.
     *
     * @author Doris Arzuaga
     * @param userRepository repositorio de usuarios
     * @param passwordEncoder codificador BCrypt de contraseñas
     * @param jwtUtils utilidades de generación de tokens
     * @param institutionalDomains dominios de correo institucional separados por coma; vacío para no restringir
     */
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

    /**
     * Registra un nuevo estudiante validando el consentimiento de datos y el correo institucional.
     *
     * @author Doris Arzuaga
     * @param request datos de registro
     * @return identificador del usuario creado
     * @throws UnprocessableEntityException cuando el consentimiento no fue otorgado
     * @throws BadRequestException cuando el correo no pertenece a un dominio institucional
     * @throws ConflictException cuando el correo ya está registrado
     */
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

    /**
     * Autentica al usuario. Tras 5 intentos fallidos bloquea la cuenta por 15 minutos. Cualquier
     * fallo responde con el mismo mensaje, sin indicar qué dato fue incorrecto. No es transaccional
     * a propósito: el registro del intento fallido debe persistir aunque se lance la excepción.
     *
     * @author Doris Arzuaga
     * @param request credenciales del usuario
     * @return tokens de acceso y refresco junto con los datos del usuario
     * @throws UnauthorizedException cuando las credenciales son inválidas, la cuenta está inactiva o bloqueada
     */
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

    /**
     * Genera un nuevo access token a partir de un refresh token válido y vigente en base de datos.
     * El refresh token no se rota.
     *
     * @author Doris Arzuaga
     * @param request refresh token recibido
     * @return nuevo access token junto con el mismo refresh token
     * @throws UnauthorizedException cuando el token es inválido, expiró, fue invalidado o la cuenta está inactiva
     */
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

    /**
     * Cierra la sesión invalidando todos los refresh tokens activos del usuario.
     *
     * @author Doris Arzuaga
     * @param userId identificador del usuario autenticado
     */
    public void logout(UUID userId) {
        userRepository.invalidateRefreshTokens(userId);
    }

    /**
     * Inicia la recuperación de contraseña. Si el correo existe genera un token de un solo uso
     * con vigencia de 30 minutos, almacena su hash SHA-256 y lo registra en el log, porque en
     * desarrollo no se envían correos reales. No revela si el correo existe.
     *
     * @author Doris Arzuaga
     * @param request correo del usuario
     */
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

    /**
     * Establece una nueva contraseña consumiendo un token de recuperación. También reinicia el
     * bloqueo de la cuenta e invalida sus sesiones activas.
     *
     * @author Doris Arzuaga
     * @param request token de recuperación y nueva contraseña
     * @throws BadRequestException cuando el token no existe, expiró o ya fue usado
     */
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

    /**
     * Obtiene el perfil del usuario autenticado.
     *
     * @author Doris Arzuaga
     * @param userId identificador del usuario autenticado
     * @return datos del perfil
     * @throws NotFoundException cuando el usuario no existe
     */
    public UserResponse getProfile(UUID userId) {
        return UserResponse.from(findUser(userId));
    }

    /**
     * Actualiza los campos editables del perfil: nombre, apellido, programa y teléfono.
     *
     * @author Doris Arzuaga
     * @param userId identificador del usuario autenticado
     * @param request nuevos datos del perfil
     * @return perfil actualizado
     * @throws NotFoundException cuando el usuario no existe
     */
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
