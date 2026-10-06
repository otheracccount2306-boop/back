package co.edu.ucc.orientacion.repositories;

import co.edu.ucc.orientacion.models.Servicio;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ServiceRepository {

    private static final RowMapper<Servicio> SERVICIO_MAPPER = (rs, i) -> new Servicio(
            rs.getObject("id", UUID.class),
            rs.getString("nombre"),
            rs.getString("descripcion"),
            rs.getString("categoria"),
            rs.getString("edificio"),
            rs.getString("horario"),
            rs.getString("contacto"),
            rs.getBoolean("activo"),
            rs.getObject("creado_en", LocalDateTime.class));

    private final NamedParameterJdbcTemplate jdbc;

    public ServiceRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Servicio> search(Collection<String> categorias, String categoria, String pattern) {
        return jdbc.query("""
                SELECT * FROM servicio
                WHERE activo = TRUE
                  AND categoria IN (:categorias)
                  AND (CAST(:categoria AS VARCHAR) IS NULL OR categoria = :categoria)
                  AND (CAST(:pattern AS VARCHAR) IS NULL
                       OR unaccent(nombre) ILIKE unaccent(CAST(:pattern AS TEXT)))
                ORDER BY nombre
                """,
                new MapSqlParameterSource()
                        .addValue("categorias", categorias)
                        .addValue("categoria", categoria)
                        .addValue("pattern", pattern),
                SERVICIO_MAPPER);
    }

    public Optional<Servicio> findActiveById(UUID id) {
        return jdbc.query("SELECT * FROM servicio WHERE id = :id AND activo = TRUE",
                new MapSqlParameterSource("id", id), SERVICIO_MAPPER).stream().findFirst();
    }

    public Optional<Servicio> findById(UUID id) {
        return jdbc.query("SELECT * FROM servicio WHERE id = :id",
                new MapSqlParameterSource("id", id), SERVICIO_MAPPER).stream().findFirst();
    }

    public List<Servicio> findAll(Collection<String> categorias, String categoria) {
        return jdbc.query("""
                SELECT * FROM servicio
                WHERE categoria IN (:categorias)
                  AND (CAST(:categoria AS VARCHAR) IS NULL OR categoria = :categoria)
                ORDER BY activo DESC, nombre
                """,
                new MapSqlParameterSource().addValue("categorias", categorias).addValue("categoria", categoria),
                SERVICIO_MAPPER);
    }

    public Servicio create(Servicio servicio) {
        return jdbc.queryForObject("""
                INSERT INTO servicio (nombre, descripcion, categoria, edificio, horario, contacto, activo)
                VALUES (:nombre, :descripcion, :categoria, :edificio, :horario, :contacto, :activo)
                RETURNING *
                """, params(servicio), SERVICIO_MAPPER);
    }

    public Servicio update(Servicio servicio) {
        return jdbc.queryForObject("""
                UPDATE servicio
                SET nombre = :nombre, descripcion = :descripcion, categoria = :categoria,
                    edificio = :edificio, horario = :horario, contacto = :contacto, activo = :activo
                WHERE id = :id
                RETURNING *
                """, params(servicio).addValue("id", servicio.id()), SERVICIO_MAPPER);
    }

    public int deactivate(UUID id) {
        return jdbc.update("UPDATE servicio SET activo = FALSE WHERE id = :id AND activo = TRUE",
                new MapSqlParameterSource("id", id));
    }

    private MapSqlParameterSource params(Servicio s) {
        return new MapSqlParameterSource()
                .addValue("nombre", s.nombre())
                .addValue("descripcion", s.descripcion())
                .addValue("categoria", s.categoria())
                .addValue("edificio", s.edificio())
                .addValue("horario", s.horario())
                .addValue("contacto", s.contacto())
                .addValue("activo", s.activo());
    }
}
