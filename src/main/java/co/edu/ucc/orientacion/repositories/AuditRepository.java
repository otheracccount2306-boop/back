package co.edu.ucc.orientacion.repositories;

import co.edu.ucc.orientacion.models.Auditoria;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Acceso a datos de la tabla de auditoría.
 *
 * @author Doris Arzuaga
 */
@Repository
public class AuditRepository {

    private static final RowMapper<Auditoria> AUDIT_MAPPER = (rs, i) -> new Auditoria(
            rs.getObject("id", UUID.class),
            rs.getObject("usuario_id", UUID.class),
            rs.getString("accion"),
            rs.getString("entidad"),
            rs.getObject("entidad_id", UUID.class),
            rs.getString("detalle"),
            rs.getObject("ejecutado_en", LocalDateTime.class));

    private final NamedParameterJdbcTemplate jdbc;

    /**
     * Crea el repositorio con la plantilla JDBC.
     *
     * @author Doris Arzuaga
     * @param jdbc plantilla JDBC con parámetros nombrados
     */
    public AuditRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Inserta un registro de auditoría.
     *
     * @author Doris Arzuaga
     * @param usuarioId usuario que ejecutó la acción
     * @param accion nombre de la acción
     * @param entidad entidad afectada
     * @param entidadId identificador de la entidad afectada
     * @param detalleJson detalle de la acción en formato JSON
     * @param ejecutadoEn instante de ejecución
     * @return número de filas insertadas
     */
    public int insert(
            UUID usuarioId, String accion, String entidad, UUID entidadId, String detalleJson, LocalDateTime ejecutadoEn) {
        return jdbc.update("""
                INSERT INTO auditoria (usuario_id, accion, entidad, entidad_id, detalle, ejecutado_en)
                VALUES (:usuarioId, :accion, :entidad, :entidadId, CAST(:detalle AS JSONB), :ejecutadoEn)
                """,
                new MapSqlParameterSource()
                        .addValue("usuarioId", usuarioId)
                        .addValue("accion", accion)
                        .addValue("entidad", entidad)
                        .addValue("entidadId", entidadId)
                        .addValue("detalle", detalleJson)
                        .addValue("ejecutadoEn", ejecutadoEn));
    }

    /**
     * Lista los registros de auditoría de una entidad, del más reciente al más antiguo.
     *
     * @author Doris Arzuaga
     * @param entidad nombre de la entidad
     * @param entidadId identificador de la entidad
     * @return registros de auditoría encontrados
     */
    public List<Auditoria> findByEntidad(String entidad, UUID entidadId) {
        return jdbc.query("""
                SELECT id, usuario_id, accion, entidad, entidad_id, CAST(detalle AS TEXT) AS detalle, ejecutado_en
                FROM auditoria
                WHERE entidad = :entidad AND entidad_id = :entidadId
                ORDER BY ejecutado_en DESC
                """,
                new MapSqlParameterSource().addValue("entidad", entidad).addValue("entidadId", entidadId),
                AUDIT_MAPPER);
    }
}
