CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS unaccent;

CREATE TABLE usuario (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  nombre VARCHAR(100) NOT NULL,
  apellido VARCHAR(100) NOT NULL,
  correo VARCHAR(150) NOT NULL UNIQUE,
  contrasena_hash VARCHAR(255) NOT NULL,
  programa_academico VARCHAR(150),
  telefono VARCHAR(20),
  rol VARCHAR(20) NOT NULL DEFAULT 'ESTUDIANTE'
    CHECK (rol IN ('ESTUDIANTE','ADMINISTRADOR')),
  activo BOOLEAN NOT NULL DEFAULT TRUE,
  consentimiento_datos BOOLEAN NOT NULL DEFAULT FALSE,
  consentimiento_fecha TIMESTAMP,
  intentos_fallidos INTEGER DEFAULT 0,
  bloqueado_hasta TIMESTAMP,
  creado_en TIMESTAMP DEFAULT NOW(),
  actualizado_en TIMESTAMP DEFAULT NOW()
);

CREATE TABLE token_recuperacion (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  usuario_id UUID NOT NULL REFERENCES usuario(id),
  token_hash VARCHAR(255) NOT NULL,
  expira_en TIMESTAMP NOT NULL,
  usado BOOLEAN NOT NULL DEFAULT FALSE,
  creado_en TIMESTAMP DEFAULT NOW()
);

CREATE TABLE token_refresco (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  usuario_id UUID NOT NULL REFERENCES usuario(id),
  token_hash VARCHAR(255) NOT NULL UNIQUE,
  expira_en TIMESTAMP NOT NULL,
  invalidado BOOLEAN NOT NULL DEFAULT FALSE,
  creado_en TIMESTAMP DEFAULT NOW()
);

CREATE TABLE asignatura (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  nombre VARCHAR(150) NOT NULL,
  codigo VARCHAR(30) NOT NULL UNIQUE,
  docente VARCHAR(150),
  aula VARCHAR(50),
  dias VARCHAR(100),
  hora_inicio TIME,
  hora_fin TIME,
  periodo_academico VARCHAR(20) NOT NULL,
  activo BOOLEAN NOT NULL DEFAULT TRUE,
  creado_en TIMESTAMP DEFAULT NOW()
);

CREATE TABLE matricula (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  usuario_id UUID NOT NULL REFERENCES usuario(id),
  asignatura_id UUID NOT NULL REFERENCES asignatura(id),
  periodo_academico VARCHAR(20) NOT NULL,
  creado_en TIMESTAMP DEFAULT NOW(),
  UNIQUE(usuario_id, asignatura_id, periodo_academico)
);

CREATE TABLE evento_calendario (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  nombre VARCHAR(200) NOT NULL,
  descripcion TEXT,
  categoria VARCHAR(50) NOT NULL,
  fecha_inicio DATE NOT NULL,
  fecha_fin DATE,
  activo BOOLEAN NOT NULL DEFAULT TRUE,
  creado_en TIMESTAMP DEFAULT NOW()
);

CREATE TABLE servicio (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  nombre VARCHAR(150) NOT NULL,
  descripcion TEXT,
  categoria VARCHAR(50) NOT NULL,
  edificio VARCHAR(100),
  horario VARCHAR(200),
  contacto VARCHAR(150),
  activo BOOLEAN NOT NULL DEFAULT TRUE,
  creado_en TIMESTAMP DEFAULT NOW()
);

CREATE TABLE espacio (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  nombre VARCHAR(150) NOT NULL,
  codigo VARCHAR(30) NOT NULL UNIQUE,
  categoria VARCHAR(50) NOT NULL,
  edificio VARCHAR(100),
  piso VARCHAR(30),
  descripcion TEXT,
  referencia TEXT,
  activo BOOLEAN NOT NULL DEFAULT TRUE,
  creado_en TIMESTAMP DEFAULT NOW()
);

CREATE TABLE noticia (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  titulo VARCHAR(250) NOT NULL,
  resumen VARCHAR(500),
  contenido TEXT NOT NULL,
  categoria VARCHAR(50) NOT NULL,
  imagen_url VARCHAR(500),
  estado VARCHAR(20) NOT NULL DEFAULT 'BORRADOR'
    CHECK (estado IN ('BORRADOR','PUBLICADO','ARCHIVADO')),
  publicado_en TIMESTAMP,
  creado_por UUID NOT NULL REFERENCES usuario(id),
  creado_en TIMESTAMP DEFAULT NOW(),
  actualizado_en TIMESTAMP DEFAULT NOW()
);

CREATE TABLE evento (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  nombre VARCHAR(200) NOT NULL,
  descripcion TEXT,
  categoria VARCHAR(50) NOT NULL,
  lugar VARCHAR(200),
  fecha_hora TIMESTAMP NOT NULL,
  cupos INTEGER,
  estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO'
    CHECK (estado IN ('ACTIVO','CONCLUIDO','CANCELADO')),
  creado_por UUID NOT NULL REFERENCES usuario(id),
  creado_en TIMESTAMP DEFAULT NOW()
);

CREATE TABLE faq (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  pregunta VARCHAR(500) NOT NULL,
  respuesta TEXT NOT NULL,
  categoria VARCHAR(50) NOT NULL,
  frecuencia INTEGER DEFAULT 0,
  activo BOOLEAN NOT NULL DEFAULT TRUE,
  creado_en TIMESTAMP DEFAULT NOW()
);

CREATE TABLE auditoria (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  usuario_id UUID REFERENCES usuario(id),
  accion VARCHAR(100) NOT NULL,
  entidad VARCHAR(100) NOT NULL,
  entidad_id UUID,
  detalle JSONB,
  ejecutado_en TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_token_recuperacion_hash ON token_recuperacion(token_hash);
CREATE INDEX idx_token_refresco_usuario ON token_refresco(usuario_id);
CREATE INDEX idx_matricula_usuario ON matricula(usuario_id);
CREATE INDEX idx_noticia_estado ON noticia(estado, publicado_en DESC);
CREATE INDEX idx_evento_estado_fecha ON evento(estado, fecha_hora);
CREATE INDEX idx_auditoria_entidad ON auditoria(entidad, entidad_id);
