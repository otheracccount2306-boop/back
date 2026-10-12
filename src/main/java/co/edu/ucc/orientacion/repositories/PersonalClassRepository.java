package co.edu.ucc.orientacion.repositories;

import co.edu.ucc.orientacion.models.ClasePersonal;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PersonalClassRepository {

    private static final RowMapper<ClasePersonal> MAPPER = (rs, i) -> new ClasePersonal(
            rs.getObject("id", UUID.class),
            rs.getObject("usuario_id", UUID.class),
            rs.getString("nombre"),
            rs.getString("docente"),
            rs.getString("aula"),
            rs.getString("dias"),
            rs.getObject("hora_inicio", LocalTime.class),
            rs.getObject("hora_fin", LocalTime.class),
            rs.getObject("creado_en", LocalDateTime.class),
            rs.getObject("actualizado_en", LocalDateTime.class));

    private final NamedParameterJdbcTemplate jdbc;

    public PersonalClassRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<ClasePersonal> findByUsuario(UUID usuarioId, String day) {
        return jdbc.query("""
                SELECT * FROM clase_personal
                WHERE usuario_id = :usuarioId
                  AND (CAST(:day AS VARCHAR) IS NULL OR CAST(:day AS VARCHAR) = ANY(string_to_array(dias, ',')))
                ORDER BY hora_inicio, nombre
                """,
                new MapSqlParameterSource().addValue("usuarioId", usuarioId).addValue("day", day), MAPPER);
    }

    public Optional<ClasePersonal> findByIdAndUsuario(UUID id, UUID usuarioId) {
        return jdbc.query("SELECT * FROM clase_personal WHERE id = :id AND usuario_id = :usuarioId",
                new MapSqlParameterSource().addValue("id", id).addValue("usuarioId", usuarioId), MAPPER)
                .stream().findFirst();
    }

    public int countByUsuario(UUID usuarioId) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM clase_personal WHERE usuario_id = :usuarioId",
                new MapSqlParameterSource("usuarioId", usuarioId), Integer.class);
        return count == null ? 0 : count;
    }

    public Optional<String> findOverlap(UUID usuarioId, String dias, LocalTime horaInicio, LocalTime horaFin,
            UUID excludeId) {
        return jdbc.query("""
                SELECT nombre FROM (
                    SELECT c.nombre, c.dias, c.hora_inicio, c.hora_fin FROM clase_personal c
                    WHERE c.usuario_id = :usuarioId
                      AND (CAST(:excludeId AS UUID) IS NULL OR c.id <> CAST(:excludeId AS UUID))
                    UNION ALL
                    SELECT a.nombre, a.dias, a.hora_inicio, a.hora_fin FROM asignatura a
                    JOIN matricula m ON m.asignatura_id = a.id
                    WHERE m.usuario_id = :usuarioId AND a.activo = TRUE
                ) clases
                WHERE string_to_array(dias, ',') && string_to_array(CAST(:dias AS TEXT), ',')
                  AND hora_inicio < :horaFin
                  AND hora_fin > :horaInicio
                ORDER BY hora_inicio
                LIMIT 1
                """,
                new MapSqlParameterSource()
                        .addValue("usuarioId", usuarioId)
                        .addValue("dias", dias)
                        .addValue("horaInicio", horaInicio)
                        .addValue("horaFin", horaFin)
                        .addValue("excludeId", excludeId),
                (rs, i) -> rs.getString("nombre")).stream().findFirst();
    }

    public ClasePersonal create(ClasePersonal clase) {
        return jdbc.queryForObject("""
                INSERT INTO clase_personal (usuario_id, nombre, docente, aula, dias, hora_inicio, hora_fin)
                VALUES (:usuarioId, :nombre, :docente, :aula, :dias, :horaInicio, :horaFin)
                RETURNING *
                """, params(clase), MAPPER);
    }

    public Optional<ClasePersonal> update(ClasePersonal clase) {
        return jdbc.query("""
                UPDATE clase_personal
                SET nombre = :nombre, docente = :docente, aula = :aula, dias = :dias,
                    hora_inicio = :horaInicio, hora_fin = :horaFin, actualizado_en = NOW()
                WHERE id = :id AND usuario_id = :usuarioId
                RETURNING *
                """, params(clase).addValue("id", clase.id()), MAPPER).stream().findFirst();
    }

    public int delete(UUID id, UUID usuarioId) {
        return jdbc.update("DELETE FROM clase_personal WHERE id = :id AND usuario_id = :usuarioId",
                new MapSqlParameterSource().addValue("id", id).addValue("usuarioId", usuarioId));
    }

    private MapSqlParameterSource params(ClasePersonal c) {
        return new MapSqlParameterSource()
                .addValue("usuarioId", c.usuarioId())
                .addValue("nombre", c.nombre())
                .addValue("docente", c.docente())
                .addValue("aula", c.aula())
                .addValue("dias", c.dias())
                .addValue("horaInicio", c.horaInicio())
                .addValue("horaFin", c.horaFin());
    }
}
