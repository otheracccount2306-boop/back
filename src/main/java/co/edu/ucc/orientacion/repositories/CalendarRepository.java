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

/**
 * Acceso a datos del calendario académico institucional.
 *
 * @author Diego Luna
 */
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

    /**
     * Crea el repositorio con la plantilla JDBC.
     *
     * @author Diego Luna
     * @param jdbc plantilla JDBC con parámetros nombrados
     */
    public CalendarRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Lista los eventos activos del calendario, opcionalmente filtrados por categoría.
     *
     * @author Diego Luna
     * @param categoria categoría normalizada, o null para no filtrar
     * @return eventos ordenados por fecha de inicio
     */
    public List<EventoCalendario> findActive(String categoria) {
        return jdbc.query("""
                SELECT * FROM evento_calendario
                WHERE activo = TRUE
                  AND (CAST(:categoria AS VARCHAR) IS NULL OR categoria = :categoria)
                ORDER BY fecha_inicio, nombre
                """,
                new MapSqlParameterSource("categoria", categoria), CALENDAR_MAPPER);
    }

    /**
     * Busca un evento activo del calendario por su identificador.
     *
     * @author Diego Luna
     * @param id identificador del evento
     * @return evento encontrado o vacío
     */
    public Optional<EventoCalendario> findActiveById(UUID id) {
        return jdbc.query("SELECT * FROM evento_calendario WHERE id = :id AND activo = TRUE",
                new MapSqlParameterSource("id", id), CALENDAR_MAPPER).stream().findFirst();
    }

    /**
     * Inserta un evento del calendario. Se ignoran el identificador, el estado y la fecha de
     * creación del modelo recibido.
     *
     * @author Diego Luna
     * @param evento datos del evento a crear
     * @return evento creado
     */
    public EventoCalendario create(EventoCalendario evento) {
        return jdbc.queryForObject("""
                INSERT INTO evento_calendario (nombre, descripcion, categoria, fecha_inicio, fecha_fin)
                VALUES (:nombre, :descripcion, :categoria, :fechaInicio, :fechaFin)
                RETURNING *
                """, params(evento), CALENDAR_MAPPER);
    }

    /**
     * Actualiza un evento activo del calendario.
     *
     * @author Diego Luna
     * @param evento evento con los nuevos datos, identificado por su id
     * @return evento actualizado
     */
    public EventoCalendario update(EventoCalendario evento) {
        return jdbc.queryForObject("""
                UPDATE evento_calendario
                SET nombre = :nombre, descripcion = :descripcion, categoria = :categoria,
                    fecha_inicio = :fechaInicio, fecha_fin = :fechaFin
                WHERE id = :id
                RETURNING *
                """, params(evento).addValue("id", evento.id()), CALENDAR_MAPPER);
    }

    /**
     * Desactiva lógicamente un evento del calendario.
     *
     * @author Diego Luna
     * @param id identificador del evento
     * @return número de filas afectadas
     */
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
