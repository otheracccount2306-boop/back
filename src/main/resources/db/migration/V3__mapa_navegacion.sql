ALTER TABLE plano ADD COLUMN IF NOT EXISTS navegacion JSONB;

COMMENT ON COLUMN plano.navegacion IS
  'Malla caminable y entradas del campus para calcular caminos en la app (ver V3__mapa_navegacion.sql)';

ALTER TABLE plano DROP CONSTRAINT IF EXISTS ck_plano_navegacion;
ALTER TABLE plano ADD CONSTRAINT ck_plano_navegacion CHECK (
  navegacion IS NULL OR (
    jsonb_typeof(navegacion) = 'object'
    AND jsonb_typeof(navegacion -> 'malla') = 'string'
    AND (navegacion ->> 'ancho')::INT > 0
    AND (navegacion ->> 'alto')::INT > 0
  )
);
