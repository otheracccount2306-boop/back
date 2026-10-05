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

/**
 * Acceso a datos de eventos institucionales.
 *
 * @author Gabriela Zabaleta
 */
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

    /**
     * Crea el repositorio con la plantilla JDBC.
     *
     * @author Gabriela Zabaleta
     * @param jdbc plantilla JDBC con parámetros nombrados
     */
    public EventRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Marca como CONCLUIDO los eventos activos cuya fecha es anterior al inicio del día indicado.
     * Los eventos no se eliminan.
     *
     * @author Gabriela Zabaleta
     * @param startOfToday inicio del día actual
     * @return número de eventos concluidos
     */
    public int concludePast(LocalDateTime startOfToday) {
        return jdbc.update(
                "UPDATE evento SET estado = 'CONCLUIDO' WHERE estado = 'ACTIVO' AND fecha_hora < :startOfToday",
                new MapSqlParameterSource("startOfToday", startOfToday));
    }

    /**
     * Lista eventos activos dentro de un rango de fechas, del más próximo al más lejano.
     *
     * @author Gabriela Zabaleta
     * @param categoria categoría normalizada, o null para no filtrar
     * @param from límite inferior inclusivo
     * @param toExclusive límite superior exclusivo, o null para no acotar
     * @param limit cantidad máxima de resultados
     * @param offset desplazamiento inicial
     * @return eventos de la página solicitada
     */
    public List<Evento> findUpcoming(
            String categoria, LocalDateTime from, LocalDateTime toExclusive, int limit, int offset) {
        return jdbc.query("SELECT * FROM evento " + UPCOMING_FILTER
                        + " ORDER BY fecha_hora, nombre LIMIT :limit OFFSET :offset",
                rangeParams(categoria, from, toExclusive).addValue("limit", limit).addValue("offset", offset),
                EVENTO_MAPPER);
    }

    /**
     * Cuenta los eventos activos dentro de un rango de fechas.
     *
     * @author Gabriela Zabaleta
     * @param categoria categoría normalizada, o null para no filtrar
     * @param from límite inferior inclusivo
     * @param toExclusive límite superior exclusivo, o null para no acotar
     * @return total de eventos
     */
    public long countUpcoming(String categoria, LocalDateTime from, LocalDateTime toExclusive) {
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM evento " + UPCOMING_FILTER,
                rangeParams(categoria, from, toExclusive), Long.class);
        return total == null ? 0 : total;
    }

    /**
     * Lista eventos de cualquier estado y fecha para la administración, del más reciente al más antiguo.
     *
     * @author Gabriela Zabaleta
     * @param categoria categoría normalizada, o null para no filtrar
     * @param estado ACTIVO, CONCLUIDO o CANCELADO; null para todos los estados
     * @param limit cantidad máxima de resultados
     * @param offset desplazamiento inicial
     * @return eventos de la página solicitada
     */
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

    /**
     * Cuenta los eventos de cualquier estado y fecha para la administración.
     *
     * @author Gabriela Zabaleta
     * @param categoria categoría normalizada, o null para no filtrar
     * @param estado ACTIVO, CONCLUIDO o CANCELADO; null para todos los estados
     * @return total de eventos que cumplen el filtro
     */
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

    /**
     * Busca un evento por su identificador sin importar su estado.
     *
     * @author Gabriela Zabaleta
     * @param id identificador del evento
     * @return evento encontrado o vacío
     */
    public Optional<Evento> findById(UUID id) {
        return jdbc.query("SELECT * FROM evento WHERE id = :id",
                new MapSqlParameterSource("id", id), EVENTO_MAPPER).stream().findFirst();
    }

    /**
     * Inserta un evento con los datos del modelo recibido.
     *
     * @author Gabriela Zabaleta
     * @param evento datos del evento a crear, incluido el autor
     * @return evento creado
     */
    public Evento create(Evento evento) {
        return jdbc.queryForObject("""
                INSERT INTO evento (nombre, descripcion, categoria, lugar, fecha_hora, cupos, estado, creado_por)
                VALUES (:nombre, :descripcion, :categoria, :lugar, :fechaHora, :cupos, :estado, :creadoPor)
                RETURNING *
                """, params(evento).addValue("creadoPor", evento.creadoPor()), EVENTO_MAPPER);
    }

    /**
     * Actualiza los datos y el estado de un evento.
     *
     * @author Gabriela Zabaleta
     * @param evento evento con los nuevos datos, identificado por su id
     * @return evento actualizado
     */
    public Evento update(Evento evento) {
        return jdbc.queryForObject("""
                UPDATE evento
                SET nombre = :nombre, descripcion = :descripcion, categoria = :categoria, lugar = :lugar,
                    fecha_hora = :fechaHora, cupos = :cupos, estado = :estado
                WHERE id = :id
                RETURNING *
                """, params(evento).addValue("id", evento.id()), EVENTO_MAPPER);
    }

    /**
     * Cancela lógicamente un evento activo cambiando su estado a CANCELADO. Los eventos
     * concluidos conservan su estado.
     *
     * @author Gabriela Zabaleta
     * @param id identificador del evento
     * @return número de filas afectadas
     */
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
