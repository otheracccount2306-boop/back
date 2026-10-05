package co.edu.ucc.orientacion.repositories;

import co.edu.ucc.orientacion.models.TokenRecuperacion;
import co.edu.ucc.orientacion.models.Usuario;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Acceso a datos de usuarios y de sus tokens de recuperación de contraseña y de refresco.
 *
 * @author Doris Arzuaga
 */
@Repository
public class UserRepository {

    private static final RowMapper<Usuario> USUARIO_MAPPER = (rs, i) -> new Usuario(
            rs.getObject("id", UUID.class),
            rs.getString("nombre"),
            rs.getString("apellido"),
            rs.getString("correo"),
            rs.getString("contrasena_hash"),
            rs.getString("programa_academico"),
            rs.getString("telefono"),
            rs.getString("rol"),
            rs.getBoolean("activo"),
            rs.getBoolean("consentimiento_datos"),
            rs.getObject("consentimiento_fecha", LocalDateTime.class),
            rs.getInt("intentos_fallidos"),
            rs.getObject("bloqueado_hasta", LocalDateTime.class),
            rs.getObject("creado_en", LocalDateTime.class),
            rs.getObject("actualizado_en", LocalDateTime.class));

    private static final RowMapper<TokenRecuperacion> RECOVERY_TOKEN_MAPPER = (rs, i) -> new TokenRecuperacion(
            rs.getObject("id", UUID.class),
            rs.getObject("usuario_id", UUID.class),
            rs.getString("token_hash"),
            rs.getObject("expira_en", LocalDateTime.class),
            rs.getBoolean("usado"),
            rs.getObject("creado_en", LocalDateTime.class));

    private static final String SEARCH_FILTER = """
            WHERE (CAST(:pattern AS VARCHAR) IS NULL
                OR unaccent(nombre) ILIKE unaccent(CAST(:pattern AS TEXT))
                OR unaccent(apellido) ILIKE unaccent(CAST(:pattern AS TEXT))
                OR unaccent(nombre || ' ' || apellido) ILIKE unaccent(CAST(:pattern AS TEXT))
                OR unaccent(correo) ILIKE unaccent(CAST(:pattern AS TEXT))
                OR unaccent(programa_academico) ILIKE unaccent(CAST(:pattern AS TEXT)))
              AND (CAST(:active AS BOOLEAN) IS NULL OR activo = :active)
            """;

    private final NamedParameterJdbcTemplate jdbc;

    /**
     * Crea el repositorio con la plantilla JDBC.
     *
     * @author Doris Arzuaga
     * @param jdbc plantilla JDBC con parámetros nombrados
     */
    public UserRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Busca un usuario por su identificador.
     *
     * @author Doris Arzuaga
     * @param id identificador del usuario
     * @return usuario encontrado o vacío
     */
    public Optional<Usuario> findById(UUID id) {
        return jdbc.query("SELECT * FROM usuario WHERE id = :id",
                new MapSqlParameterSource("id", id), USUARIO_MAPPER).stream().findFirst();
    }

    /**
     * Busca un usuario por su correo.
     *
     * @author Doris Arzuaga
     * @param correo correo normalizado en minúsculas
     * @return usuario encontrado o vacío
     */
    public Optional<Usuario> findByCorreo(String correo) {
        return jdbc.query("SELECT * FROM usuario WHERE correo = :correo",
                new MapSqlParameterSource("correo", correo), USUARIO_MAPPER).stream().findFirst();
    }

    /**
     * Verifica si ya existe un usuario con el correo indicado.
     *
     * @author Doris Arzuaga
     * @param correo correo normalizado en minúsculas
     * @return true si el correo ya está registrado
     */
    public boolean existsByCorreo(String correo) {
        Boolean exists = jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM usuario WHERE correo = :correo)",
                new MapSqlParameterSource("correo", correo), Boolean.class);
        return Boolean.TRUE.equals(exists);
    }

    /**
     * Inserta un nuevo usuario con rol ESTUDIANTE.
     *
     * @author Doris Arzuaga
     * @param nombre nombres
     * @param apellido apellidos
     * @param correo correo normalizado en minúsculas
     * @param contrasenaHash hash BCrypt de la contraseña
     * @param programaAcademico programa académico
     * @param telefono teléfono de contacto
     * @param consentimientoFecha fecha en que se otorgó el consentimiento de datos
     * @return identificador del usuario creado
     */
    public UUID create(
            String nombre,
            String apellido,
            String correo,
            String contrasenaHash,
            String programaAcademico,
            String telefono,
            LocalDateTime consentimientoFecha) {
        return jdbc.queryForObject("""
                INSERT INTO usuario (nombre, apellido, correo, contrasena_hash, programa_academico, telefono,
                                     rol, consentimiento_datos, consentimiento_fecha)
                VALUES (:nombre, :apellido, :correo, :contrasenaHash, :programa, :telefono,
                        'ESTUDIANTE', TRUE, :consentimientoFecha)
                RETURNING id
                """,
                new MapSqlParameterSource()
                        .addValue("nombre", nombre)
                        .addValue("apellido", apellido)
                        .addValue("correo", correo)
                        .addValue("contrasenaHash", contrasenaHash)
                        .addValue("programa", programaAcademico)
                        .addValue("telefono", telefono)
                        .addValue("consentimientoFecha", consentimientoFecha),
                (rs, i) -> rs.getObject("id", UUID.class));
    }

    /**
     * Actualiza los campos editables del perfil.
     *
     * @author Doris Arzuaga
     * @param id identificador del usuario
     * @param nombre nuevos nombres
     * @param apellido nuevos apellidos
     * @param programaAcademico nuevo programa académico
     * @param telefono nuevo teléfono
     * @param now fecha de actualización
     * @return número de filas afectadas
     */
    public int updateProfile(
            UUID id, String nombre, String apellido, String programaAcademico, String telefono, LocalDateTime now) {
        return jdbc.update("""
                UPDATE usuario
                SET nombre = :nombre, apellido = :apellido, programa_academico = :programa,
                    telefono = :telefono, actualizado_en = :now
                WHERE id = :id
                """,
                new MapSqlParameterSource()
                        .addValue("id", id)
                        .addValue("nombre", nombre)
                        .addValue("apellido", apellido)
                        .addValue("programa", programaAcademico)
                        .addValue("telefono", telefono)
                        .addValue("now", now));
    }

    /**
     * Registra un intento de inicio de sesión fallido de forma atómica. Al alcanzar el máximo
     * bloquea la cuenta hasta la fecha indicada y reinicia el contador.
     *
     * @author Doris Arzuaga
     * @param id identificador del usuario
     * @param maxAttempts número de intentos fallidos que provoca el bloqueo
     * @param lockUntil fecha hasta la cual se bloquea la cuenta
     * @return número de filas afectadas
     */
    public int registerFailedLogin(UUID id, int maxAttempts, LocalDateTime lockUntil) {
        return jdbc.update("""
                UPDATE usuario
                SET intentos_fallidos = CASE WHEN COALESCE(intentos_fallidos, 0) + 1 >= :max
                                             THEN 0 ELSE COALESCE(intentos_fallidos, 0) + 1 END,
                    bloqueado_hasta = CASE WHEN COALESCE(intentos_fallidos, 0) + 1 >= :max
                                           THEN :lockUntil ELSE bloqueado_hasta END
                WHERE id = :id
                """,
                new MapSqlParameterSource()
                        .addValue("id", id)
                        .addValue("max", maxAttempts)
                        .addValue("lockUntil", lockUntil));
    }

    /**
     * Reinicia el contador de intentos fallidos y elimina el bloqueo de la cuenta.
     *
     * @author Doris Arzuaga
     * @param id identificador del usuario
     * @return número de filas afectadas
     */
    public int resetLoginState(UUID id) {
        return jdbc.update(
                "UPDATE usuario SET intentos_fallidos = 0, bloqueado_hasta = NULL WHERE id = :id",
                new MapSqlParameterSource("id", id));
    }

    /**
     * Actualiza el hash de la contraseña de un usuario.
     *
     * @author Doris Arzuaga
     * @param id identificador del usuario
     * @param contrasenaHash nuevo hash BCrypt
     * @param now fecha de actualización
     * @return número de filas afectadas
     */
    public int updatePassword(UUID id, String contrasenaHash, LocalDateTime now) {
        return jdbc.update(
                "UPDATE usuario SET contrasena_hash = :hash, actualizado_en = :now WHERE id = :id",
                new MapSqlParameterSource().addValue("id", id).addValue("hash", contrasenaHash).addValue("now", now));
    }

    /**
     * Lista usuarios de forma paginada filtrando por nombre, apellido, correo o programa.
     *
     * @author Doris Arzuaga
     * @param pattern patrón de búsqueda, o null para no filtrar; la búsqueda ignora tildes y mayúsculas
     * @param active true para solo cuentas activas, false para solo inactivas, null para todas
     * @param limit cantidad máxima de resultados
     * @param offset desplazamiento inicial
     * @return usuarios de la página solicitada
     */
    public List<Usuario> search(String pattern, Boolean active, int limit, int offset) {
        return jdbc.query("SELECT * FROM usuario " + SEARCH_FILTER
                        + " ORDER BY apellido, nombre LIMIT :limit OFFSET :offset",
                new MapSqlParameterSource()
                        .addValue("pattern", pattern)
                        .addValue("active", active)
                        .addValue("limit", limit)
                        .addValue("offset", offset),
                USUARIO_MAPPER);
    }

    /**
     * Cuenta los usuarios que cumplen el filtro de búsqueda.
     *
     * @author Doris Arzuaga
     * @param pattern patrón de búsqueda, o null para no filtrar; la búsqueda ignora tildes y mayúsculas
     * @param active true para solo cuentas activas, false para solo inactivas, null para todas
     * @return total de usuarios
     */
    public long countSearch(String pattern, Boolean active) {
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM usuario " + SEARCH_FILTER,
                new MapSqlParameterSource().addValue("pattern", pattern).addValue("active", active),
                Long.class);
        return total == null ? 0 : total;
    }

    /**
     * Activa o desactiva una cuenta.
     *
     * @author Doris Arzuaga
     * @param id identificador del usuario
     * @param activo nuevo estado de la cuenta
     * @param now fecha de actualización
     * @return número de filas afectadas
     */
    public int updateActive(UUID id, boolean activo, LocalDateTime now) {
        return jdbc.update("UPDATE usuario SET activo = :activo, actualizado_en = :now WHERE id = :id",
                new MapSqlParameterSource().addValue("id", id).addValue("activo", activo).addValue("now", now));
    }

    /**
     * Cambia el rol de un usuario.
     *
     * @author Doris Arzuaga
     * @param id identificador del usuario
     * @param rol nuevo rol
     * @param now fecha de actualización
     * @return número de filas afectadas
     */
    public int updateRole(UUID id, String rol, LocalDateTime now) {
        return jdbc.update("UPDATE usuario SET rol = :rol, actualizado_en = :now WHERE id = :id",
                new MapSqlParameterSource().addValue("id", id).addValue("rol", rol).addValue("now", now));
    }

    /**
     * Elimina definitivamente a un usuario y sus datos personales asociados (derecho de supresión
     * de la Ley 1581 de 2012). Las noticias y eventos creados por el usuario se reasignan al
     * administrador indicado y los registros de auditoría del usuario se anonimizan.
     *
     * @author Doris Arzuaga
     * @param id identificador del usuario a eliminar
     * @param reassignTo administrador que hereda la autoría del contenido institucional
     * @return número de usuarios eliminados
     */
    public int deletePermanently(UUID id, UUID reassignTo) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("admin", reassignTo);
        jdbc.update("DELETE FROM token_recuperacion WHERE usuario_id = :id", params);
        jdbc.update("DELETE FROM token_refresco WHERE usuario_id = :id", params);
        jdbc.update("DELETE FROM matricula WHERE usuario_id = :id", params);
        jdbc.update("UPDATE noticia SET creado_por = :admin WHERE creado_por = :id", params);
        jdbc.update("UPDATE evento SET creado_por = :admin WHERE creado_por = :id", params);
        jdbc.update("UPDATE auditoria SET usuario_id = NULL WHERE usuario_id = :id", params);
        return jdbc.update("DELETE FROM usuario WHERE id = :id", params);
    }

    /**
     * Almacena el hash de un refresh token emitido.
     *
     * @author Doris Arzuaga
     * @param userId usuario propietario del token
     * @param tokenHash hash SHA-256 del refresh token
     * @param expiresAt fecha de expiración del token
     * @return número de filas insertadas
     */
    public int saveRefreshToken(UUID userId, String tokenHash, LocalDateTime expiresAt) {
        return jdbc.update("""
                INSERT INTO token_refresco (usuario_id, token_hash, expira_en)
                VALUES (:userId, :hash, :expiresAt)
                """,
                new MapSqlParameterSource()
                        .addValue("userId", userId)
                        .addValue("hash", tokenHash)
                        .addValue("expiresAt", expiresAt));
    }

    /**
     * Busca el usuario dueño de un refresh token que no esté invalidado ni expirado.
     *
     * @author Doris Arzuaga
     * @param tokenHash hash SHA-256 del refresh token
     * @param now instante actual
     * @return identificador del usuario o vacío si el token no es válido
     */
    public Optional<UUID> findUserIdByValidRefreshToken(String tokenHash, LocalDateTime now) {
        return jdbc.query("""
                SELECT usuario_id FROM token_refresco
                WHERE token_hash = :hash AND invalidado = FALSE AND expira_en > :now
                """,
                new MapSqlParameterSource().addValue("hash", tokenHash).addValue("now", now),
                (rs, i) -> rs.getObject("usuario_id", UUID.class)).stream().findFirst();
    }

    /**
     * Invalida todos los refresh tokens activos de un usuario.
     *
     * @author Doris Arzuaga
     * @param userId identificador del usuario
     * @return número de tokens invalidados
     */
    public int invalidateRefreshTokens(UUID userId) {
        return jdbc.update(
                "UPDATE token_refresco SET invalidado = TRUE WHERE usuario_id = :userId AND invalidado = FALSE",
                new MapSqlParameterSource("userId", userId));
    }

    /**
     * Almacena el hash de un token de recuperación de contraseña.
     *
     * @author Doris Arzuaga
     * @param userId usuario propietario del token
     * @param tokenHash hash SHA-256 del token
     * @param expiresAt fecha de expiración del token
     * @return número de filas insertadas
     */
    public int createRecoveryToken(UUID userId, String tokenHash, LocalDateTime expiresAt) {
        return jdbc.update("""
                INSERT INTO token_recuperacion (usuario_id, token_hash, expira_en)
                VALUES (:userId, :hash, :expiresAt)
                """,
                new MapSqlParameterSource()
                        .addValue("userId", userId)
                        .addValue("hash", tokenHash)
                        .addValue("expiresAt", expiresAt));
    }

    /**
     * Busca un token de recuperación que no esté usado ni expirado.
     *
     * @author Doris Arzuaga
     * @param tokenHash hash SHA-256 del token
     * @param now instante actual
     * @return token encontrado o vacío
     */
    public Optional<TokenRecuperacion> findValidRecoveryToken(String tokenHash, LocalDateTime now) {
        return jdbc.query("""
                SELECT * FROM token_recuperacion
                WHERE token_hash = :hash AND usado = FALSE AND expira_en > :now
                """,
                new MapSqlParameterSource().addValue("hash", tokenHash).addValue("now", now),
                RECOVERY_TOKEN_MAPPER).stream().findFirst();
    }

    /**
     * Marca un token de recuperación como usado. La condición sobre el estado garantiza que
     * solo una solicitud concurrente pueda consumirlo.
     *
     * @author Doris Arzuaga
     * @param id identificador del token
     * @return true si este llamado consumió el token
     */
    public boolean markRecoveryTokenUsed(UUID id) {
        return jdbc.update("UPDATE token_recuperacion SET usado = TRUE WHERE id = :id AND usado = FALSE",
                new MapSqlParameterSource("id", id)) == 1;
    }

    /**
     * Marca como usados todos los tokens de recuperación pendientes de un usuario.
     *
     * @author Doris Arzuaga
     * @param userId identificador del usuario
     * @return número de tokens invalidados
     */
    public int invalidateRecoveryTokens(UUID userId) {
        return jdbc.update(
                "UPDATE token_recuperacion SET usado = TRUE WHERE usuario_id = :userId AND usado = FALSE",
                new MapSqlParameterSource("userId", userId));
    }
}
