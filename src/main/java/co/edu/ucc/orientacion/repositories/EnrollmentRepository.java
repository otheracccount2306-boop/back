package co.edu.ucc.orientacion.repositories;

import co.edu.ucc.orientacion.models.Asignatura;
import co.edu.ucc.orientacion.models.Matricula;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public class EnrollmentRepository {

    private static final RowMapper<Matricula> MATRICULA_MAPPER = (rs, i) -> new Matricula(
            rs.getObject("id", UUID.class),
            rs.getObject("usuario_id", UUID.class),
            rs.getObject("asignatura_id", UUID.class),
            rs.getString("periodo_academico"),
            rs.getObject("creado_en", LocalDateTime.class));

    private final NamedParameterJdbcTemplate jdbc;

    public EnrollmentRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Asignatura> findScheduleByUsuario(UUID usuarioId, String day) {
        return jdbc.query("""
                SELECT a.* FROM asignatura a
                JOIN matricula m ON m.asignatura_id = a.id
                WHERE m.usuario_id = :usuarioId
                  AND a.activo = TRUE
                  AND (CAST(:day AS VARCHAR) IS NULL
                       OR CAST(:day AS VARCHAR) = ANY(string_to_array(a.dias, ',')))
                ORDER BY a.hora_inicio, a.nombre
                """,
                new MapSqlParameterSource().addValue("usuarioId", usuarioId).addValue("day", day),
                SubjectRepository.ASIGNATURA_MAPPER);
    }

    public List<Matricula> findByUsuario(UUID usuarioId) {
        return jdbc.query("SELECT * FROM matricula WHERE usuario_id = :usuarioId ORDER BY creado_en",
                new MapSqlParameterSource("usuarioId", usuarioId), MATRICULA_MAPPER);
    }

    public boolean exists(UUID usuarioId, UUID asignaturaId, String periodoAcademico) {
        Boolean exists = jdbc.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM matricula
                    WHERE usuario_id = :usuarioId AND asignatura_id = :asignaturaId AND periodo_academico = :periodo)
                """,
                new MapSqlParameterSource()
                        .addValue("usuarioId", usuarioId)
                        .addValue("asignaturaId", asignaturaId)
                        .addValue("periodo", periodoAcademico),
                Boolean.class);
        return Boolean.TRUE.equals(exists);
    }

    public Matricula save(UUID usuarioId, UUID asignaturaId, String periodoAcademico) {
        return jdbc.queryForObject("""
                INSERT INTO matricula (usuario_id, asignatura_id, periodo_academico)
                VALUES (:usuarioId, :asignaturaId, :periodo)
                RETURNING *
                """,
                new MapSqlParameterSource()
                        .addValue("usuarioId", usuarioId)
                        .addValue("asignaturaId", asignaturaId)
                        .addValue("periodo", periodoAcademico),
                MATRICULA_MAPPER);
    }
}
