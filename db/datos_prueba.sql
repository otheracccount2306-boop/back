CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO usuario (nombre, apellido, correo, contrasena_hash, programa_academico,
                     telefono, rol, activo, consentimiento_datos, consentimiento_fecha)
VALUES
    ('Admin',      'UCC',      'admin@campusucc.edu.co',
     crypt('Test1234!', gen_salt('bf', 12)),
     'Administración', '3001000000', 'ADMINISTRADOR', TRUE, TRUE, NOW()),

    ('Juan',       'Pérez',    'juan.perez@campusucc.edu.co',
     crypt('Test1234!', gen_salt('bf', 12)),
     'Ingeniería de Software', '3012000001', 'ESTUDIANTE', TRUE, TRUE, NOW()),

    ('María',      'López',    'maria.lopez@campusucc.edu.co',
     crypt('Test1234!', gen_salt('bf', 12)),
     'Ingeniería de Software', '3012000002', 'ESTUDIANTE', TRUE, TRUE, NOW()),

    ('Carlos',     'García',   'carlos.garcia@campusucc.edu.co',
     crypt('Test1234!', gen_salt('bf', 12)),
     'Administración de Empresas', '3012000003', 'ESTUDIANTE', TRUE, TRUE, NOW());

INSERT INTO asignatura (nombre, codigo, docente, aula, dias, hora_inicio, hora_fin, periodo_academico)
VALUES
    ('Ingeniería de Software II', 'ISW-201', 'Prof. García Martínez',
     'Aula 3 201', 'LUNES,MIERCOLES', '14:00', '16:00', '2026-1'),

    ('Base de Datos I',            'BDD-201', 'Prof. López Hernández',
     'Sala de Cómputo 2', 'MARTES,JUEVES',  '16:00', '18:00', '2026-1'),

    ('Matemáticas Discretas',      'MAT-101', 'Prof. Rodríguez Silva',
     'Aula 2 201', 'LUNES,MIERCOLES,VIERNES', '07:00', '09:00', '2026-1'),

    ('Algoritmos y Programación',  'ALG-102', 'Prof. Martínez Díaz',
     'Sala de Cómputo 1', 'MARTES,JUEVES',  '10:00', '12:00', '2026-1'),

    ('Comunicación Oral y Escrita','COM-101', 'Prof. Vargas Pérez',
     'Aula 2 105', 'VIERNES',        '08:00', '10:00', '2026-1');

INSERT INTO matricula (usuario_id, asignatura_id, periodo_academico)
SELECT u.id, a.id, '2026-1'
FROM   usuario u
CROSS  JOIN asignatura a
WHERE  u.correo = 'juan.perez@campusucc.edu.co';

INSERT INTO matricula (usuario_id, asignatura_id, periodo_academico)
SELECT u.id, a.id, '2026-1'
FROM   usuario u, asignatura a
WHERE  u.correo = 'maria.lopez@campusucc.edu.co'
  AND  a.codigo IN ('ISW-201', 'BDD-201');

INSERT INTO evento_calendario (nombre, descripcion, categoria, fecha_inicio, fecha_fin)
VALUES
    ('Inicio de clases 2026-1',
     'Inicio oficial del semestre académico 2026-1.',
     'INICIO_CLASES', '2026-02-03', '2026-02-03'),

    ('Primer examen parcial',
     'Semana de evaluaciones parciales — todas las asignaturas.',
     'EXAMENES', '2026-03-17', '2026-03-21'),

    ('Semana de receso universitario',
     'Semana de descanso institucional. No hay clases.',
     'RECESOS', '2026-04-07', '2026-04-11'),

    ('Inscripción de materias 2026-2',
     'Periodo habilitado para inscribir materias del siguiente semestre.',
     'INSCRIPCIONES', '2026-04-28', '2026-05-09'),

    ('Segundo examen parcial',
     'Semana de evaluaciones parciales finales.',
     'EXAMENES', '2026-05-04', '2026-05-08'),

    ('Exámenes finales 2026-1',
     'Periodo de evaluaciones finales del semestre.',
     'EXAMENES', '2026-06-02', '2026-06-13'),

    ('Cierre de semestre 2026-1',
     'Fecha límite de entrega de notas y cierre administrativo.',
     'INSCRIPCIONES', '2026-06-20', '2026-06-20');

INSERT INTO servicio (nombre, descripcion, categoria, edificio, horario, contacto)
VALUES
    ('Apoyo psicológico',
     'Servicio de orientación y acompañamiento psicológico para estudiantes que requieran apoyo emocional o académico.',
     'PSICOLOGIA', 'Bloque D · Piso 2', 'Lunes a Viernes 8:00 AM – 5:00 PM', 'bienestar@campusucc.edu.co'),

    ('Enfermería',
     'Atención básica en salud, primeros auxilios y orientación médica para la comunidad universitaria.',
     'SALUD', 'Bloque A · Piso 1', 'Lunes a Viernes 7:00 AM – 4:00 PM', 'enfermeria@campusucc.edu.co'),

    ('Actividad física y deporte',
     'Programas de entrenamiento, torneos internos y actividades recreativas deportivas abiertas a toda la comunidad universitaria.',
     'DEPORTE', 'Canchas principales', 'Lunes a Sábado 6:00 AM – 8:00 PM', 'deportes@campusucc.edu.co'),

    ('Cultura y arte',
     'Actividades culturales, grupos artísticos, teatro, música y danza para el desarrollo integral del estudiante.',
     'CULTURA', 'Auditorio principal', 'Martes y Jueves 2:00 PM – 6:00 PM', 'cultura@campusucc.edu.co'),

    ('Pastoral universitaria',
     'Acompañamiento espiritual y formación en valores para la comunidad universitaria.',
     'PASTORAL', 'Capilla universitaria', 'Lunes a Viernes 8:00 AM – 12:00 PM', 'pastoral@campusucc.edu.co'),

    ('Becas y apoyos económicos',
     'Información y gestión de becas, auxilios económicos y descuentos de matrícula disponibles para estudiantes.',
     'BECAS', 'Bloque Admin · Piso 1', 'Lunes a Viernes 8:00 AM – 4:00 PM', 'becas@campusucc.edu.co');

INSERT INTO servicio (nombre, descripcion, categoria, edificio, horario, contacto)
VALUES
    ('Admisiones y Registro Académico',
     'Gestión de matrículas, cancelaciones, certificados y demás trámites académicos oficiales.',
     'DEPARTAMENTO', 'Bloque Admin · Piso 1', 'Lunes a Viernes 8:00 AM – 5:00 PM', 'registro@campusucc.edu.co'),

    ('Tesorería y Pagos',
     'Liquidación y pago de matrículas, recibos de servicios y trámites financieros institucionales.',
     'DEPARTAMENTO', 'Bloque Admin · Piso 2', 'Lunes a Viernes 8:00 AM – 4:00 PM', 'tesoreria@campusucc.edu.co'),

    ('Biblioteca',
     'Préstamo de material bibliográfico, sala de estudio, acceso a bases de datos académicas y recursos digitales.',
     'DEPARTAMENTO', 'Bloque B · Piso 1 y 2', 'Lunes a Viernes 7:00 AM – 8:00 PM | Sábados 8:00 AM – 1:00 PM', 'biblioteca@campusucc.edu.co'),

    ('Soporte de Sistemas y TI',
     'Soporte técnico para plataformas institucionales, correo universitario y sistemas académicos.',
     'DEPARTAMENTO', 'Bloque C · Piso 1', 'Lunes a Viernes 8:00 AM – 5:00 PM', 'sistemas@campusucc.edu.co');

INSERT INTO espacio (nombre, codigo, categoria, edificio, piso, descripcion, referencia)
VALUES
    ('Aula 101', 'AU-101', 'AULA', 'Bloque A', 'Piso 1',
     'Aula de clases con capacidad para 40 estudiantes, equipada con videobeam y tablero acrílico.',
     'Entrada principal del Bloque A, primera puerta a la derecha.'),

    ('Aula 201', 'AU-201', 'AULA', 'Bloque A', 'Piso 2',
     'Aula de clases con capacidad para 35 estudiantes, equipada con videobeam.',
     'Subir escaleras del Bloque A, segunda puerta a la izquierda.'),

    ('Aula 301', 'AU-301', 'AULA', 'Bloque B', 'Piso 3',
     'Aula de clases con capacidad para 40 estudiantes. Ideal para clases teóricas de ingeniería.',
     'Bloque B, tercer piso, primera puerta al fondo del pasillo.'),

    ('Laboratorio de Sistemas 101', 'LAB-101', 'LABORATORIO', 'Bloque C', 'Piso 1',
     'Sala de cómputo con 30 equipos de escritorio, acceso a internet y software especializado.',
     'Bloque C, piso 1. Puerta con cartel "Laboratorio de Sistemas".'),

    ('Laboratorio de Sistemas 102', 'LAB-102', 'LABORATORIO', 'Bloque C', 'Piso 1',
     'Sala de cómputo con 30 equipos, especializada en bases de datos y programación avanzada.',
     'Bloque C, piso 1. Contiguo al Laboratorio 101.'),

    ('Biblioteca Central', 'BIB-001', 'BIBLIOTECA', 'Bloque B', 'Piso 1 y 2',
     'Cuenta con sala de lectura, sala de estudio grupal, sala silenciosa y colección de más de 5.000 títulos.',
     'Entrada principal Bloque B, seguir señalización hacia la biblioteca.'),

    ('Cafetería Central', 'CAF-001', 'CAFETERIA', 'Bloque Central', 'Piso 1',
     'Servicio de alimentación con menú del día, cafetería rápida y zona de descanso.',
     'Área central del campus, frente al parque principal.'),

    ('Auditorium Principal', 'AUD-001', 'AREA_COMUN', 'Bloque Cultural', 'Piso 1',
     'Auditorio con capacidad para 200 personas, tarima, sonido profesional e iluminación escénica.',
     'Bloque Cultural, al fondo del campus. Seguir señalización "Auditorio".'),

    ('Sala de Estudio Grupal', 'SAL-001', 'AREA_COMUN', 'Bloque B', 'Piso 2',
     'Sala equipada con mesas amplias, televisor y marcadores. Capacidad para 20 personas.',
     'Bloque B, piso 2, al lado de la Biblioteca.'),

    ('Cancha de Fútbol', 'DEP-001', 'AREA_COMUN', 'Zona Deportiva', 'Planta',
     'Cancha de grama sintética disponible para entrenamiento y torneos internos.',
     'Zona deportiva al costado occidental del campus.');

INSERT INTO noticia (titulo, resumen, contenido, categoria, estado, publicado_en, creado_por)
SELECT
    'Inscripciones abiertas para el segundo semestre 2026',
    'La Universidad Cooperativa de Colombia informa que ya están disponibles las inscripciones para el segundo semestre del año 2026.',
    'La Universidad Cooperativa de Colombia, campus Santa Marta, informa a toda su comunidad estudiantil que a partir del 28 de abril de 2026 se encuentran abiertas las inscripciones de materias para el segundo semestre del año 2026. Los estudiantes podrán acceder al portal académico institucional para realizar el proceso de selección de asignaturas dentro de las fechas establecidas en el calendario académico. Se recomienda revisar con anticipación los requisitos de cada programa para evitar inconvenientes durante el proceso.',
    'INSTITUCIONAL',
    'PUBLICADO',
    NOW() - INTERVAL '2 hours',
    id
FROM usuario WHERE correo = 'admin@campusucc.edu.co';

INSERT INTO noticia (titulo, resumen, contenido, categoria, estado, publicado_en, creado_por)
SELECT
    'Semana de Ingeniería 2026: convocatoria abierta',
    'El programa de Ingeniería de Software invita a toda la comunidad universitaria a participar en la Semana de Ingeniería 2026.',
    'El programa de Ingeniería de Software de la Universidad Cooperativa de Colombia, campus Santa Marta, tiene el gusto de anunciar la realización de la Semana de Ingeniería 2026, que se llevará a cabo del 25 al 29 de abril del presente año. El evento contará con conferencias magistrales, talleres técnicos, hackathon y feria de proyectos estudiantiles. La participación es abierta a todos los estudiantes de la institución. Más información en la secretaría del programa.',
    'ACADEMICO',
    'PUBLICADO',
    NOW() - INTERVAL '1 day',
    id
FROM usuario WHERE correo = 'admin@campusucc.edu.co';

INSERT INTO noticia (titulo, resumen, contenido, categoria, estado, creado_por)
SELECT
    'Nueva sala de estudio disponible 24 horas [BORRADOR]',
    'La Biblioteca Central anuncia la apertura de una nueva sala de estudio con acceso extendido.',
    'Contenido en revisión. Pendiente de aprobación por comunicaciones institucionales.',
    'INSTITUCIONAL',
    'BORRADOR',
    id
FROM usuario WHERE correo = 'admin@campusucc.edu.co';

INSERT INTO evento (nombre, descripcion, categoria, lugar, fecha_hora, cupos, estado, creado_por)
SELECT
    'Semana de Ingeniería 2026',
    'Conferencias, talleres técnicos, hackathon y feria de proyectos estudiantiles. Entrada libre para toda la comunidad universitaria.',
    'ACADEMICO',
    'Auditorio principal y Bloque C',
    NOW() + INTERVAL '5 days',
    200,
    'ACTIVO',
    id
FROM usuario WHERE correo = 'admin@campusucc.edu.co';

INSERT INTO evento (nombre, descripcion, categoria, lugar, fecha_hora, cupos, estado, creado_por)
SELECT
    'Feria de Emprendimiento UCC 2026',
    'Muestra de proyectos emprendedores de estudiantes de todos los programas. Abierto al público general.',
    'INSTITUCIONAL',
    'Plaza Central del campus',
    NOW() + INTERVAL '9 days',
    500,
    'ACTIVO',
    id
FROM usuario WHERE correo = 'admin@campusucc.edu.co';

INSERT INTO evento (nombre, descripcion, categoria, lugar, fecha_hora, cupos, estado, creado_por)
SELECT
    'Torneo deportivo interno 2026-1',
    'Torneos de fútbol, baloncesto y voleibol. Inscripciones en la oficina de Bienestar Universitario.',
    'DEPORTE',
    'Zona deportiva — canchas principales',
    NOW() + INTERVAL '14 days',
    150,
    'ACTIVO',
    id
FROM usuario WHERE correo = 'admin@campusucc.edu.co';

INSERT INTO evento (nombre, descripcion, categoria, lugar, fecha_hora, cupos, estado, creado_por)
SELECT
    'Charla: Oportunidades laborales en tecnología [PASADO]',
    'Charla con empresas del sector tecnológico de Santa Marta. Evento ya realizado.',
    'ACADEMICO',
    'Auditorio principal',
    NOW() - INTERVAL '10 days',
    100,
    'CONCLUIDO',
    id
FROM usuario WHERE correo = 'admin@campusucc.edu.co';

INSERT INTO faq (pregunta, respuesta, categoria, frecuencia)
VALUES
    ('¿Cómo me matriculo al siguiente semestre?',
     'Debes ingresar al portal académico institucional con tus credenciales durante las fechas de inscripción establecidas en el calendario académico. Selecciona las asignaturas disponibles para tu programa y semestre, y confirma el proceso antes del cierre del período.',
     'MATRICULAS', 45),

    ('¿Dónde pago la matrícula?',
     'El pago de matrícula se realiza en la oficina de Tesorería (Bloque Admin, Piso 2) o mediante transferencia bancaria a la cuenta institucional indicada en la liquidación generada por el portal académico. El comprobante debe entregarse en Tesorería dentro de los plazos establecidos.',
     'PAGOS', 38),

    ('¿Cómo accedo al correo institucional?',
     'Tu correo institucional se activa al formalizar la matrícula. Ingresa a mail.ucc.edu.co con el usuario y contraseña temporal entregada por la Oficina de Sistemas. En tu primer ingreso se te solicitará cambiar la contraseña.',
     'SISTEMAS', 32),

    ('¿Qué hago si tengo problemas con el portal académico?',
     'Comunícate con la Oficina de Soporte de Sistemas en el Bloque C, Piso 1, o escribe a sistemas@campusucc.edu.co indicando tu nombre, programa académico y descripción del problema.',
     'SISTEMAS', 29),

    ('¿Cómo solicito una certificación de estudio?',
     'Las certificaciones de estudio se solicitan en la Oficina de Admisiones y Registro Académico (Bloque Admin, Piso 1). Debes presentar tu carné estudiantil y diligenciar el formulario de solicitud. El tiempo de entrega es de 3 días hábiles.',
     'TRAMITES', 27),

    ('¿Dónde queda la sala de sistemas?',
     'Los laboratorios de sistemas se encuentran en el Bloque C, Piso 1. El Laboratorio 101 y el Laboratorio 102 están disponibles en horario de clases y durante horas libres según disponibilidad.',
     'CAMPUS', 24),

    ('¿Cómo accedo a los servicios de bienestar universitario?',
     'Los servicios de bienestar (psicología, enfermería, deporte y cultura) están disponibles en el Bloque D, Piso 2. También puedes escribir a bienestar@campusucc.edu.co para agendar citas o conocer la programación de actividades.',
     'BIENESTAR', 21),

    ('¿Puedo cancelar una matrícula después del plazo?',
     'Las cancelaciones extemporáneas están sujetas a autorización del Comité de Permanencia. Debes radicar una solicitud en la Oficina de Registro Académico con los soportes que justifiquen la situación. El proceso puede tardar entre 5 y 10 días hábiles.',
     'MATRICULAS', 19),

    ('¿Cómo obtengo el carné estudiantil?',
     'El carné estudiantil se solicita en la Oficina de Admisiones y Registro Académico una vez formalizada la matrícula. Trae una fotografía reciente en fondo blanco tamaño 3x4 y el recibo de pago de matrícula.',
     'TRAMITES', 15),

    ('¿Hay servicio de préstamo de equipos portátiles?',
     'La Biblioteca Central cuenta con un servicio de préstamo de computadores portátiles por período académico. Debes acercarte con tu carné estudiantil y diligenciar el formulario de préstamo. Sujeto a disponibilidad.',
     'BIENESTAR', 12);

SELECT 'usuarios'          AS tabla, COUNT(*) AS registros FROM usuario
UNION ALL
SELECT 'asignaturas',                COUNT(*) FROM asignatura
UNION ALL
SELECT 'matriculas',                 COUNT(*) FROM matricula
UNION ALL
SELECT 'eventos_calendario',         COUNT(*) FROM evento_calendario
UNION ALL
SELECT 'servicios',                  COUNT(*) FROM servicio
UNION ALL
SELECT 'espacios',                   COUNT(*) FROM espacio
UNION ALL
SELECT 'noticias',                   COUNT(*) FROM noticia
UNION ALL
SELECT 'eventos',                    COUNT(*) FROM evento
UNION ALL
SELECT 'faq',                        COUNT(*) FROM faq
ORDER BY tabla;
