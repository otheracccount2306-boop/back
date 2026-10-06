package co.edu.ucc.orientacion.repositories;

import co.edu.ucc.orientacion.models.Faq;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class FaqRepository {

    private static final RowMapper<Faq> FAQ_MAPPER = (rs, i) -> new Faq(
            rs.getObject("id", UUID.class),
            rs.getString("pregunta"),
            rs.getString("respuesta"),
            rs.getString("categoria"),
            rs.getInt("frecuencia"),
            rs.getBoolean("activo"),
            rs.getObject("creado_en", LocalDateTime.class));

    private final NamedParameterJdbcTemplate jdbc;

    public FaqRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Faq> search(String categoria, String pattern) {
        return jdbc.query("""
                SELECT * FROM faq
                WHERE activo = TRUE
                  AND (CAST(:categoria AS VARCHAR) IS NULL OR categoria = :categoria)
                  AND (CAST(:pattern AS VARCHAR) IS NULL
                       OR unaccent(pregunta) ILIKE unaccent(CAST(:pattern AS TEXT))
                       OR unaccent(respuesta) ILIKE unaccent(CAST(:pattern AS TEXT)))
                ORDER BY frecuencia DESC, pregunta
                """,
                new MapSqlParameterSource().addValue("categoria", categoria).addValue("pattern", pattern),
                FAQ_MAPPER);
    }

    public Optional<Faq> findActiveById(UUID id) {
        return jdbc.query("SELECT * FROM faq WHERE id = :id AND activo = TRUE",
                new MapSqlParameterSource("id", id), FAQ_MAPPER).stream().findFirst();
    }

    public Optional<Faq> findById(UUID id) {
        return jdbc.query("SELECT * FROM faq WHERE id = :id",
                new MapSqlParameterSource("id", id), FAQ_MAPPER).stream().findFirst();
    }

    public List<Faq> findAll(String categoria) {
        return jdbc.query("""
                SELECT * FROM faq
                WHERE (CAST(:categoria AS VARCHAR) IS NULL OR categoria = :categoria)
                ORDER BY activo DESC, frecuencia DESC, pregunta
                """,
                new MapSqlParameterSource("categoria", categoria), FAQ_MAPPER);
    }

    public Faq create(Faq faq) {
        return jdbc.queryForObject("""
                INSERT INTO faq (pregunta, respuesta, categoria, activo)
                VALUES (:pregunta, :respuesta, :categoria, :activo)
                RETURNING *
                """, params(faq), FAQ_MAPPER);
    }

    public Faq update(Faq faq) {
        return jdbc.queryForObject("""
                UPDATE faq
                SET pregunta = :pregunta, respuesta = :respuesta, categoria = :categoria, activo = :activo
                WHERE id = :id
                RETURNING *
                """, params(faq).addValue("id", faq.id()), FAQ_MAPPER);
    }

    public int deactivate(UUID id) {
        return jdbc.update("UPDATE faq SET activo = FALSE WHERE id = :id AND activo = TRUE",
                new MapSqlParameterSource("id", id));
    }

    private MapSqlParameterSource params(Faq f) {
        return new MapSqlParameterSource()
                .addValue("pregunta", f.pregunta())
                .addValue("respuesta", f.respuesta())
                .addValue("categoria", f.categoria())
                .addValue("activo", f.activo());
    }
}
