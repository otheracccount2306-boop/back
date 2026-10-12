-- ============================================================
-- Navegación por pisos
-- ============================================================
-- La navegación del plano pasa de una sola malla caminable (malla) a una malla por piso (pisos)
-- con las escaleras y ascensores que los unen (escaleras). La restricción acepta los dos
-- formatos para no romper planos guardados con el anterior.
--
-- Idempotente. Requiere V3.
--   psql -U postgres -d ucc_orientacion -1 -f src/main/resources/db/migration/V5__navegacion_por_pisos.sql
-- ============================================================

ALTER TABLE plano DROP CONSTRAINT IF EXISTS ck_plano_navegacion;
ALTER TABLE plano ADD CONSTRAINT ck_plano_navegacion CHECK (
  navegacion IS NULL OR (
    jsonb_typeof(navegacion) = 'object'
    AND (jsonb_typeof(navegacion -> 'malla') = 'string' OR jsonb_typeof(navegacion -> 'pisos') = 'object')
    AND (navegacion ->> 'ancho')::INT > 0
    AND (navegacion ->> 'alto')::INT > 0
  )
);

COMMENT ON COLUMN plano.navegacion IS
  'Mallas caminables por piso, escaleras que las unen, entradas y fondo del campus (ver V5__navegacion_por_pisos.sql)';
