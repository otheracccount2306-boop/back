package co.edu.ucc.orientacion.repositories;

import co.edu.ucc.orientacion.models.Asignatura;
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
 * Acceso a datos de asignaturas.
 *
 * @author Diego Luna
 */
@Repository
public class SubjectRepository {

    static final RowMapper<Asignatura> ASIGNATURA_MAPPER = (rs, i) -> new Asignatura(
            rs.getObject("id", UUID.class),
            rs.getString("nombre"),
            rs.getString("codigo"),
            rs.getString("docente"),
            rs.getString("aula"),
            rs.getString("dias"),
            rs.getObject("hora_inicio", LocalTime.class),
            rs.getObject("hora_fin", LocalTime.class),
            rs.getString("periodo_academico"),
            rs.getBoolean("activo"),
            rs.getObject("creado_en", LocalDateTime.class));

    private final NamedParameterJdbcTemplate jdbc;

    /**
     * Crea el repositorio con la plantilla JDBC.
     *
     * @author Diego Luna
     * @param jdbc plantilla JDBC con parámetros nombrados
     */
    public SubjectRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Busca una asignatura activa por su identificador.
     *
     * @author Diego Luna
     * @param id identificador de la asignatura
     * @return asignatura encontrada o vacío
     */
    public Optional<Asignatura> findActiveById(UUID id) {
        return jdbc.query("SELECT * FROM asignatura WHERE id = :id AND activo = TRUE",
                new MapSqlParameterSource("id", id), ASIGNATURA_MAPPER).stream().findFirst();
    }

    /**
     * Verifica si existe una asignatura con el código indicado.
     *
     * @author Diego Luna
     * @param codigo código de la asignatura
     * @param excludeId asignatura a excluir de la búsqueda, o null
     * @return true si el código ya está en uso
     */
    public boolean existsByCodigo(String codigo, UUID excludeId) {
        Boolean exists = jdbc.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM asignatura
                    WHERE codigo = :codigo
                      AND (CAST(:excludeId AS UUID) IS NULL OR id <> CAST(:excludeId AS UUID)))
                """,
                new MapSqlParameterSource().addValue("codigo", codigo).addValue("excludeId", excludeId),
                Boolean.class);
        return Boolean.TRUE.equals(exists);
    }

    /**
     * Verifica si otra asignatura activa usa la misma aula en el mismo periodo, comparte algún
     * día y se traslapa en horario.
     *
     * @author Diego Luna
     * @param aula aula a verificar
     * @param periodoAcademico periodo académico
     * @param dias días de clase separados por coma
     * @param horaInicio hora de inicio
     * @param horaFin hora de finalización
     * @param excludeId asignatura a excluir de la búsqueda, o null
     * @return true si existe un conflicto de aula y horario
     */
    public boolean hasConflict(
            String aula,
            String periodoAcademico,
            String dias,
            LocalTime horaInicio,
            LocalTime horaFin,
            UUID excludeId) {
        Boolean exists = jdbc.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM asignatura
                    WHERE activo = TRUE
                      AND UPPER(aula) = UPPER(:aula)
                      AND periodo_academico = :periodo
                      AND (CAST(:excludeId AS UUID) IS NULL OR id <> CAST(:excludeId AS UUID))
                      AND string_to_array(dias, ',') && string_to_array(CAST(:dias AS TEXT), ',')
                      AND hora_inicio < :horaFin
                      AND hora_fin > :horaInicio)
                """,
                new MapSqlParameterSource()
                        .addValue("aula", aula)
                        .addValue("periodo", periodoAcademico)
                        .addValue("dias", dias)
                        .addValue("horaInicio", horaInicio)
                        .addValue("horaFin", horaFin)
                        .addValue("excludeId", excludeId),
                Boolean.class);
        return Boolean.TRUE.equals(exists);
    }

    /**
     * Busca una asignatura por su identificador sin importar si está activa.
     *
     * @author Diego Luna
     * @param id identificador de la asignatura
     * @return asignatura encontrada o vacío
     */
    public Optional<Asignatura> findById(UUID id) {
        return jdbc.query("SELECT * FROM asignatura WHERE id = :id",
                new MapSqlParameterSource("id", id), ASIGNATURA_MAPPER).stream().findFirst();
    }

    /**
     * Lista las asignaturas incluyendo las inactivas, para la administración.
     *
     * @author Diego Luna
     * @param periodoAcademico periodo académico exacto, o null para todos
     * @return asignaturas ordenadas por periodo descendente, estado y nombre
     */
    public List<Asignatura> findAll(String periodoAcademico) {
        return jdbc.query("""
                SELECT * FROM asignatura
                WHERE (CAST(:periodo AS VARCHAR) IS NULL OR periodo_academico = :periodo)
                ORDER BY periodo_academico DESC, activo DESC, nombre
                """,
                new MapSqlParameterSource("periodo", periodoAcademico), ASIGNATURA_MAPPER);
    }

    /**
     * Inserta una asignatura. Se ignoran el identificador y la fecha de creación del modelo recibido.
     *
     * @author Diego Luna
     * @param asignatura datos de la asignatura a crear, incluido su estado activo
     * @return asignatura creada
     */
    public Asignatura create(Asignatura asignatura) {
        return jdbc.queryForObject("""
                INSERT INTO asignatura (nombre, codigo, docente, aula, dias, hora_inicio, hora_fin,
                                        periodo_academico, activo)
                VALUES (:nombre, :codigo, :docente, :aula, :dias, :horaInicio, :horaFin, :periodo, :activo)
                RETURNING *
                """, params(asignatura), ASIGNATURA_MAPPER);
    }

    /**
     * Actualiza los datos de una asignatura, activa o no, incluido su estado de vigencia.
     *
     * @author Diego Luna
     * @param asignatura asignatura con los nuevos datos, identificada por su id
     * @return asignatura actualizada
     */
    public Asignatura update(Asignatura asignatura) {
        return jdbc.queryForObject("""
                UPDATE asignatura
                SET nombre = :nombre, codigo = :codigo, docente = :docente, aula = :aula, dias = :dias,
                    hora_inicio = :horaInicio, hora_fin = :horaFin, periodo_academico = :periodo,
                    activo = :activo
                WHERE id = :id
                RETURNING *
                """, params(asignatura).addValue("id", asignatura.id()), ASIGNATURA_MAPPER);
    }

    /**
     * Desactiva lógicamente una asignatura.
     *
     * @author Diego Luna
     * @param id identificador de la asignatura
     * @return número de filas afectadas
     */
    public int deactivate(UUID id) {
        return jdbc.update("UPDATE asignatura SET activo = FALSE WHERE id = :id AND activo = TRUE",
                new MapSqlParameterSource("id", id));
    }

    private MapSqlParameterSource params(Asignatura a) {
        return new MapSqlParameterSource()
                .addValue("nombre", a.nombre())
                .addValue("codigo", a.codigo())
                .addValue("docente", a.docente())
                .addValue("aula", a.aula())
                .addValue("dias", a.dias())
                .addValue("horaInicio", a.horaInicio())
                .addValue("horaFin", a.horaFin())
                .addValue("periodo", a.periodoAcademico())
                .addValue("activo", a.activo());
    }
}
