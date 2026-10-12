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
