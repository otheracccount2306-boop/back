-- ============================================================
-- Clases que registra el propio estudiante
-- ============================================================
-- Cada estudiante puede anotar sus clases (nombre, docente, aula, días y horas) para verlas en
-- su horario junto con las asignaturas en las que está matriculado. Son datos privados: solo
-- el dueño las consulta y modifica, y no afectan las asignaturas institucionales.
--
-- Idempotente. Requiere V1.
--   psql -U postgres -d ucc_orientacion -1 -f src/main/resources/db/migration/V4__clases_personales.sql
-- ============================================================

CREATE TABLE IF NOT EXISTS clase_personal (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  usuario_id UUID NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
  nombre VARCHAR(150) NOT NULL,
  docente VARCHAR(150),
  aula VARCHAR(50),
  dias VARCHAR(100) NOT NULL,
  hora_inicio TIME NOT NULL,
  hora_fin TIME NOT NULL,
  creado_en TIMESTAMP NOT NULL DEFAULT NOW(),
  actualizado_en TIMESTAMP NOT NULL DEFAULT NOW(),
  CONSTRAINT ck_clase_personal_horas CHECK (hora_fin > hora_inicio),
  CONSTRAINT ck_clase_personal_nombre CHECK (length(btrim(nombre)) > 0),
  CONSTRAINT ck_clase_personal_dias CHECK (dias ~ '^(LUNES|MARTES|MIERCOLES|JUEVES|VIERNES|SABADO|DOMINGO)(,(LUNES|MARTES|MIERCOLES|JUEVES|VIERNES|SABADO|DOMINGO))*$')
);

COMMENT ON TABLE clase_personal IS 'Clases que el estudiante registra para su propio horario';

CREATE INDEX IF NOT EXISTS idx_clase_personal_usuario ON clase_personal (usuario_id, hora_inicio);
