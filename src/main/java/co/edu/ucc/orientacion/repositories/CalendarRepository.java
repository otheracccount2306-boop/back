package co.edu.ucc.orientacion.repositories;

import co.edu.ucc.orientacion.models.EventoCalendario;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class CalendarRepository {

    private static final RowMapper<EventoCalendario> CALENDAR_MAPPER = (rs, i) -> new EventoCalendario(
            rs.getObject("id", UUID.class),
            rs.getString("nombre"),
            rs.getString("descripcion"),
            rs.getString("categoria"),
            rs.getObject("fecha_inicio", LocalDate.class),
            rs.getObject("fecha_fin", LocalDate.class),
            rs.getBoolean("activo"),
            rs.getObject("creado_en", LocalDateTime.class));

    private final NamedParameterJdbcTemplate jdbc;

    public CalendarRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<EventoCalendario> findActive(String categoria) {
        return jdbc.query("""
                SELECT * FROM evento_calendario
                WHERE activo = TRUE
                  AND (CAST(:categoria AS VARCHAR) IS NULL OR categoria = :categoria)
                ORDER BY fecha_inicio, nombre
                """,
                new MapSqlParameterSource("categoria", categoria), CALENDAR_MAPPER);
    }

    public Optional<EventoCalendario> findActiveById(UUID id) {
        return jdbc.query("SELECT * FROM evento_calendario WHERE id = :id AND activo = TRUE",
                new MapSqlParameterSource("id", id), CALENDAR_MAPPER).stream().findFirst();
    }

    public EventoCalendario create(EventoCalendario evento) {
        return jdbc.queryForObject("""
                INSERT INTO evento_calendario (nombre, descripcion, categoria, fecha_inicio, fecha_fin)
                VALUES (:nombre, :descripcion, :categoria, :fechaInicio, :fechaFin)
                RETURNING *
                """, params(evento), CALENDAR_MAPPER);
    }

    public EventoCalendario update(EventoCalendario evento) {
        return jdbc.queryForObject("""
                UPDATE evento_calendario
                SET nombre = :nombre, descripcion = :descripcion, categoria = :categoria,
                    fecha_inicio = :fechaInicio, fecha_fin = :fechaFin
                WHERE id = :id
                RETURNING *
                """, params(evento).addValue("id", evento.id()), CALENDAR_MAPPER);
    }

    public int deactivate(UUID id) {
        return jdbc.update("UPDATE evento_calendario SET activo = FALSE WHERE id = :id AND activo = TRUE",
                new MapSqlParameterSource("id", id));
    }

    private MapSqlParameterSource params(EventoCalendario e) {
        return new MapSqlParameterSource()
                .addValue("nombre", e.nombre())
                .addValue("descripcion", e.descripcion())
                .addValue("categoria", e.categoria())
                .addValue("fechaInicio", e.fechaInicio())
                .addValue("fechaFin", e.fechaFin());
    }
}
