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

    public UserRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Usuario> findById(UUID id) {
        return jdbc.query("SELECT * FROM usuario WHERE id = :id",
                new MapSqlParameterSource("id", id), USUARIO_MAPPER).stream().findFirst();
    }

    public Optional<Usuario> findByCorreo(String correo) {
        return jdbc.query("SELECT * FROM usuario WHERE correo = :correo",
                new MapSqlParameterSource("correo", correo), USUARIO_MAPPER).stream().findFirst();
    }

    public boolean existsByCorreo(String correo) {
        Boolean exists = jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM usuario WHERE correo = :correo)",
                new MapSqlParameterSource("correo", correo), Boolean.class);
        return Boolean.TRUE.equals(exists);
    }

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

    public int resetLoginState(UUID id) {
        return jdbc.update(
                "UPDATE usuario SET intentos_fallidos = 0, bloqueado_hasta = NULL WHERE id = :id",
                new MapSqlParameterSource("id", id));
    }

    public int updatePassword(UUID id, String contrasenaHash, LocalDateTime now) {
        return jdbc.update(
                "UPDATE usuario SET contrasena_hash = :hash, actualizado_en = :now WHERE id = :id",
                new MapSqlParameterSource().addValue("id", id).addValue("hash", contrasenaHash).addValue("now", now));
    }

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

    public long countSearch(String pattern, Boolean active) {
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM usuario " + SEARCH_FILTER,
                new MapSqlParameterSource().addValue("pattern", pattern).addValue("active", active),
                Long.class);
        return total == null ? 0 : total;
    }

    public int updateActive(UUID id, boolean activo, LocalDateTime now) {
        return jdbc.update("UPDATE usuario SET activo = :activo, actualizado_en = :now WHERE id = :id",
                new MapSqlParameterSource().addValue("id", id).addValue("activo", activo).addValue("now", now));
    }

    public int updateRole(UUID id, String rol, LocalDateTime now) {
        return jdbc.update("UPDATE usuario SET rol = :rol, actualizado_en = :now WHERE id = :id",
                new MapSqlParameterSource().addValue("id", id).addValue("rol", rol).addValue("now", now));
    }

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

    public Optional<UUID> findUserIdByValidRefreshToken(String tokenHash, LocalDateTime now) {
        return jdbc.query("""
                SELECT usuario_id FROM token_refresco
                WHERE token_hash = :hash AND invalidado = FALSE AND expira_en > :now
                """,
                new MapSqlParameterSource().addValue("hash", tokenHash).addValue("now", now),
                (rs, i) -> rs.getObject("usuario_id", UUID.class)).stream().findFirst();
    }

    public int invalidateRefreshTokens(UUID userId) {
        return jdbc.update(
                "UPDATE token_refresco SET invalidado = TRUE WHERE usuario_id = :userId AND invalidado = FALSE",
                new MapSqlParameterSource("userId", userId));
    }

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

    public Optional<TokenRecuperacion> findValidRecoveryToken(String tokenHash, LocalDateTime now) {
        return jdbc.query("""
                SELECT * FROM token_recuperacion
                WHERE token_hash = :hash AND usado = FALSE AND expira_en > :now
                """,
                new MapSqlParameterSource().addValue("hash", tokenHash).addValue("now", now),
                RECOVERY_TOKEN_MAPPER).stream().findFirst();
    }

    public boolean markRecoveryTokenUsed(UUID id) {
        return jdbc.update("UPDATE token_recuperacion SET usado = TRUE WHERE id = :id AND usado = FALSE",
                new MapSqlParameterSource("id", id)) == 1;
    }

    public int invalidateRecoveryTokens(UUID userId) {
        return jdbc.update(
                "UPDATE token_recuperacion SET usado = TRUE WHERE usuario_id = :userId AND usado = FALSE",
                new MapSqlParameterSource("userId", userId));
    }
}
