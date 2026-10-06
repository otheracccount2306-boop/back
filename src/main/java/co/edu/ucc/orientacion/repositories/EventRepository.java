package co.edu.ucc.orientacion.repositories;

import co.edu.ucc.orientacion.models.Evento;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class EventRepository {

    private static final RowMapper<Evento> EVENTO_MAPPER = (rs, i) -> new Evento(
            rs.getObject("id", UUID.class),
            rs.getString("nombre"),
            rs.getString("descripcion"),
            rs.getString("categoria"),
            rs.getString("lugar"),
            rs.getObject("fecha_hora", LocalDateTime.class),
            rs.getObject("cupos", Integer.class),
            rs.getString("estado"),
            rs.getObject("creado_por", UUID.class),
            rs.getObject("creado_en", LocalDateTime.class));

    private static final String UPCOMING_FILTER = """
            WHERE estado = 'ACTIVO'
              AND fecha_hora >= :from
              AND (CAST(:to AS TIMESTAMP) IS NULL OR fecha_hora < :to)
              AND (CAST(:categoria AS VARCHAR) IS NULL OR categoria = :categoria)
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public EventRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public int concludePast(LocalDateTime startOfToday) {
        return jdbc.update(
                "UPDATE evento SET estado = 'CONCLUIDO' WHERE estado = 'ACTIVO' AND fecha_hora < :startOfToday",
                new MapSqlParameterSource("startOfToday", startOfToday));
    }

    public List<Evento> findUpcoming(
            String categoria, LocalDateTime from, LocalDateTime toExclusive, int limit, int offset) {
        return jdbc.query("SELECT * FROM evento " + UPCOMING_FILTER
                        + " ORDER BY fecha_hora, nombre LIMIT :limit OFFSET :offset",
                rangeParams(categoria, from, toExclusive).addValue("limit", limit).addValue("offset", offset),
                EVENTO_MAPPER);
    }

    public long countUpcoming(String categoria, LocalDateTime from, LocalDateTime toExclusive) {
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM evento " + UPCOMING_FILTER,
                rangeParams(categoria, from, toExclusive), Long.class);
        return total == null ? 0 : total;
    }

    public List<Evento> findAllAdmin(String categoria, String estado, int limit, int offset) {
        return jdbc.query("""
                SELECT * FROM evento
                WHERE (CAST(:categoria AS VARCHAR) IS NULL OR categoria = :categoria)
                  AND (CAST(:estado AS VARCHAR) IS NULL OR estado = :estado)
                ORDER BY fecha_hora DESC, nombre
                LIMIT :limit OFFSET :offset
                """,
                new MapSqlParameterSource()
                        .addValue("categoria", categoria)
                        .addValue("estado", estado)
                        .addValue("limit", limit)
                        .addValue("offset", offset),
                EVENTO_MAPPER);
    }

    public long countAllAdmin(String categoria, String estado) {
        Long total = jdbc.queryForObject("""
                SELECT COUNT(*) FROM evento
                WHERE (CAST(:categoria AS VARCHAR) IS NULL OR categoria = :categoria)
                  AND (CAST(:estado AS VARCHAR) IS NULL OR estado = :estado)
                """,
                new MapSqlParameterSource().addValue("categoria", categoria).addValue("estado", estado),
                Long.class);
        return total == null ? 0 : total;
    }

    public Optional<Evento> findById(UUID id) {
        return jdbc.query("SELECT * FROM evento WHERE id = :id",
                new MapSqlParameterSource("id", id), EVENTO_MAPPER).stream().findFirst();
    }

    public Evento create(Evento evento) {
        return jdbc.queryForObject("""
                INSERT INTO evento (nombre, descripcion, categoria, lugar, fecha_hora, cupos, estado, creado_por)
                VALUES (:nombre, :descripcion, :categoria, :lugar, :fechaHora, :cupos, :estado, :creadoPor)
                RETURNING *
                """, params(evento).addValue("creadoPor", evento.creadoPor()), EVENTO_MAPPER);
    }

    public Evento update(Evento evento) {
        return jdbc.queryForObject("""
                UPDATE evento
                SET nombre = :nombre, descripcion = :descripcion, categoria = :categoria, lugar = :lugar,
                    fecha_hora = :fechaHora, cupos = :cupos, estado = :estado
                WHERE id = :id
                RETURNING *
                """, params(evento).addValue("id", evento.id()), EVENTO_MAPPER);
    }

    public int cancel(UUID id) {
        return jdbc.update("UPDATE evento SET estado = 'CANCELADO' WHERE id = :id AND estado = 'ACTIVO'",
                new MapSqlParameterSource("id", id));
    }

    private MapSqlParameterSource rangeParams(String categoria, LocalDateTime from, LocalDateTime toExclusive) {
        return new MapSqlParameterSource()
                .addValue("categoria", categoria)
                .addValue("from", from)
                .addValue("to", toExclusive);
    }

    private MapSqlParameterSource params(Evento e) {
        return new MapSqlParameterSource()
                .addValue("nombre", e.nombre())
                .addValue("descripcion", e.descripcion())
                .addValue("categoria", e.categoria())
                .addValue("lugar", e.lugar())
                .addValue("fechaHora", e.fechaHora())
                .addValue("cupos", e.cupos())
                .addValue("estado", e.estado());
    }
}
