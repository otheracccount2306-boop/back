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

/**
 * Acceso a datos de las clases que registra cada estudiante. Todas las consultas filtran por el
 * dueño, así que un estudiante nunca puede leer ni modificar las clases de otro.
 *
 * @author Diego Luna
 */
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

    /**
     * Crea el repositorio con la plantilla JDBC.
     *
     * @author Diego Luna
     * @param jdbc plantilla JDBC con parámetros nombrados
     */
    public PersonalClassRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Lista las clases de un estudiante, opcionalmente solo las de un día.
     *
     * @author Diego Luna
     * @param usuarioId estudiante dueño
     * @param day día normalizado, o null para todos
     * @return clases ordenadas por hora de inicio
     */
    public List<ClasePersonal> findByUsuario(UUID usuarioId, String day) {
        return jdbc.query("""
                SELECT * FROM clase_personal
                WHERE usuario_id = :usuarioId
                  AND (CAST(:day AS VARCHAR) IS NULL OR CAST(:day AS VARCHAR) = ANY(string_to_array(dias, ',')))
                ORDER BY hora_inicio, nombre
                """,
                new MapSqlParameterSource().addValue("usuarioId", usuarioId).addValue("day", day), MAPPER);
    }

    /**
     * Busca una clase de un estudiante.
     *
     * @author Diego Luna
     * @param id identificador de la clase
     * @param usuarioId estudiante dueño
     * @return la clase, o vacío si no existe o es de otro estudiante
     */
    public Optional<ClasePersonal> findByIdAndUsuario(UUID id, UUID usuarioId) {
        return jdbc.query("SELECT * FROM clase_personal WHERE id = :id AND usuario_id = :usuarioId",
                new MapSqlParameterSource().addValue("id", id).addValue("usuarioId", usuarioId), MAPPER)
                .stream().findFirst();
    }

    /**
     * Cuenta las clases registradas por un estudiante.
     *
     * @author Diego Luna
     * @param usuarioId estudiante dueño
     * @return número de clases
     */
    public int countByUsuario(UUID usuarioId) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM clase_personal WHERE usuario_id = :usuarioId",
                new MapSqlParameterSource("usuarioId", usuarioId), Integer.class);
        return count == null ? 0 : count;
    }

    /**
     * Busca una clase del mismo estudiante que se cruce en día y hora: entre las que registró y
     * entre las asignaturas activas en las que está matriculado.
     *
     * @author Diego Luna
     * @param usuarioId estudiante dueño
     * @param dias días separados por coma
     * @param horaInicio hora de inicio
     * @param horaFin hora de finalización
     * @param excludeId clase propia a excluir (la que se está editando), o null
     * @return nombre de la primera clase que se cruza, o vacío
     */
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

    /**
     * Inserta una clase. Se ignoran el identificador y las fechas del modelo recibido.
     *
     * @author Diego Luna
     * @param clase datos de la clase, con su dueño
     * @return clase creada
     */
    public ClasePersonal create(ClasePersonal clase) {
        return jdbc.queryForObject("""
                INSERT INTO clase_personal (usuario_id, nombre, docente, aula, dias, hora_inicio, hora_fin)
                VALUES (:usuarioId, :nombre, :docente, :aula, :dias, :horaInicio, :horaFin)
                RETURNING *
                """, params(clase), MAPPER);
    }

    /**
     * Actualiza una clase solo si pertenece al estudiante indicado.
     *
     * @author Diego Luna
     * @param clase nuevos datos, con su id y su dueño
     * @return clase actualizada, o vacío si no existe o es de otro estudiante
     */
    public Optional<ClasePersonal> update(ClasePersonal clase) {
        return jdbc.query("""
                UPDATE clase_personal
                SET nombre = :nombre, docente = :docente, aula = :aula, dias = :dias,
                    hora_inicio = :horaInicio, hora_fin = :horaFin, actualizado_en = NOW()
                WHERE id = :id AND usuario_id = :usuarioId
                RETURNING *
                """, params(clase).addValue("id", clase.id()), MAPPER).stream().findFirst();
    }

    /**
     * Borra una clase solo si pertenece al estudiante indicado.
     *
     * @author Diego Luna
     * @param id identificador de la clase
     * @param usuarioId estudiante dueño
     * @return número de filas borradas
     */
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
