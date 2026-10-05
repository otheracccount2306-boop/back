# Backend — App de Orientación Estudiantil UCC

Universidad Cooperativa de Colombia · Campus Santa Marta. Trabajo de grado de Ingeniería de Software, 2026.

Monolito Java 21 + Spring Boot 3.2 + Spring Security 6 (JWT stateless) + Spring JDBC + PostgreSQL.

## Puesta en marcha

1. Crear la base de datos y aplicar el esquema. El proyecto no incluye Flyway, así que el script se ejecuta a mano una sola vez:

   ```
   createdb -U postgres ucc_orientacion
   psql -U postgres -d ucc_orientacion -f src/main/resources/db/migration/V1__initial_schema.sql
   ```

2. Arrancar (requiere JDK 21 y Maven):

   ```
   mvn spring-boot:run
   ```

   La API queda en `http://localhost:8080/api/v1`.

3. Crear el primer administrador. El registro solo crea estudiantes, así que el primer administrador se promueve por SQL. Después de eso, los roles se cambian desde `PUT /api/v1/admin/users/{id}/role`:

   ```sql
   UPDATE usuario SET rol = 'ADMINISTRADOR' WHERE correo = 'correo@ucc.edu.co';
   ```

   Para el mapa del campus (planos y polígonos de los espacios) aplique también, una sola vez, la extensión V2. Es idempotente y funciona sobre una base que ya tiene datos:

   ```
   psql -U postgres -d ucc_orientacion -1 -f src/main/resources/db/migration/V2__mapa_infraestructura.sql
   ```

4. Pruebas unitarias (no requieren base de datos): `mvn test`

5. Datos de prueba (opcional): `db/datos_prueba.sql`. **En Windows, fuerce UTF-8 al cargarlo**; de lo contrario `psql` usa la codificación de la consola y las tildes quedan corruptas (`Enfermería` se guarda como `EnfermerÃ­a`, sin dar error):

   ```
   set PGCLIENTENCODING=UTF8
   psql -U postgres -d ucc_orientacion -1 -f db/datos_prueba.sql
   ```

   El `-1` ejecuta el script en una sola transacción: si algo falla, no queda cargado a medias.

## Endpoints de administración

Además de los CRUD de contenido (`POST`, `PUT`, `DELETE` bajo `/api/v1/admin/...`), el panel administrativo usa estos listados, que devuelven registros en **cualquier estado**. Todos exigen rol `ADMINISTRADOR`:

| Endpoint | Parámetros | Devuelve |
|---|---|---|
| `GET /admin/news` | `category`, `status` (BORRADOR, PUBLICADO, ARCHIVADO), `page` | Noticias completas, 10 por página |
| `GET /admin/events` | `category`, `status` (ACTIVO, CONCLUIDO, CANCELADO), `page` | Eventos de cualquier fecha, 10 por página |
| `GET /admin/services/{type}` | `category` | Servicios o preguntas, incluidos los inactivos. `type`: `wellbeing`, `departments` (o `department`) o `faq` |
| `GET /admin/campus/spaces` | `category` | Espacios activos e inactivos |
| `GET /admin/academic/subjects` | `period` (por ejemplo `2026-1`) | Asignaturas activas e inactivas |
| `GET /admin/users` | `search`, `active` (true/false), `page` | Usuarios, con filtro opcional por estado de cuenta |

Los `PUT` de servicios, preguntas frecuentes, espacios y asignaturas aceptan un campo opcional `activo` para ocultar o volver a mostrar el registro, y ahora también editan registros que están inactivos. Si se omite `activo`, se conserva el estado actual. Al dejar una asignatura activa se vuelve a validar el conflicto de aula y horario (409); mientras esté inactiva no se valida.

## Mapa del campus (módulo de infraestructura)

Los espacios se ubican sobre **planos estáticos** (una imagen por edificio o piso), sin GPS ni mapas satelitales. El mapa usa Leaflet con `L.CRS.Simple`, así que las coordenadas son **píxeles de la imagen**: `x` desde el borde izquierdo y `y` desde el borde inferior.

- Tabla `plano`: nombre, edificio, piso, imagen como data URL (PNG, JPEG o WebP, hasta ~2 MB) y su ancho y alto. El servidor lee las dimensiones de la propia imagen.
- `espacio.plano_id` y `espacio.geometria` (`JSONB`): un objeto GeoJSON `Polygon` con posiciones `[x, y]`. La base exige que sea un Polygon y que geometría y plano vayan juntos; el backend valida además anillos cerrados, mínimo tres vértices, máximo 500 y que no se salga de la imagen (ajusta al borde con 2 px de tolerancia).
- Cada cambio de polígono actualiza `plano.actualizado_en`; la app compara esa fecha para saber si su copia sin conexión sigue vigente.
- Si un plano ya tiene espacios dibujados, su imagen solo se puede reemplazar por otra del mismo tamaño (409), para no desplazar los polígonos.

| Endpoint | Rol | Devuelve |
|---|---|---|
| `GET /campus/plans` | autenticado | Planos activos, sin imagen, con `espaciosDibujados` y `actualizadoEn` |
| `GET /campus/plans/{id}` | autenticado | Plano con imagen y `espacios: [{ id, nombre, codigo, categoria, geometria }]` (solo activos) |
| `GET /admin/campus/plans` · `GET /admin/campus/plans/{id}` | ADMINISTRADOR | Igual, en cualquier estado |
| `POST /admin/campus/plans` · `PUT /admin/campus/plans/{id}` | ADMINISTRADOR | `{ nombre, edificio, piso, imagen, ancho, alto, activo }`; en `PUT`, `imagen: null` conserva la actual |
| `DELETE /admin/campus/plans/{id}` | ADMINISTRADOR | Eliminación lógica; los polígonos se conservan |
| `PUT /admin/campus/spaces/{id}/geometry` | ADMINISTRADOR | `{ planoId, geometria }` (Geometry o Feature GeoJSON). 422 si el polígono no es válido |
| `DELETE /admin/campus/spaces/{id}/geometry` | ADMINISTRADOR | Quita el espacio del mapa |

`GET /campus/spaces` y `GET /admin/campus/spaces` ahora incluyen `planoId` y `geometria` en cada espacio. El `PUT` de espacios no toca la geometría.

## Configuración

Todo se lee de `application.properties` y puede sobrescribirse con variables de entorno:

| Variable | Valor por defecto |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/ucc_orientacion` |
| `DB_USER` / `DB_PASSWORD` | `postgres` / `proyecto` |
| `SERVER_PORT` | `8080` |
| `JWT_SECRET` | secreto de desarrollo, **cambiarlo en producción** |
| `JWT_EXPIRATION` / `JWT_REFRESH_EXPIRATION` | 24 h / 7 días (milisegundos) |
| `INSTITUTIONAL_EMAIL_DOMAINS` | `campusucc.edu.co,ucc.edu.co` (vacío = no restringir) |

## Convenciones de la API

- Todas las respuestas usan `ApiResponse { success, data, message }`.
- Los endpoints, salvo los de `/auth/*` públicos, exigen `Authorization: Bearer <accessToken>`. `/admin/**` exige rol `ADMINISTRADOR`.
- Paginación: parámetro `page` que inicia en **1**, 10 elementos por página.
- Categorías y días se aceptan sin distinguir mayúsculas ni tildes (`Área común` → `AREA_COMUN`, `miércoles` → `MIERCOLES`).
- Las búsquedas de texto (espacios, directorio, preguntas frecuentes y usuarios) ignoran tildes y mayúsculas gracias a la extensión `unaccent` de PostgreSQL. Si la base de datos ya existía antes de este cambio, ejecutar una vez: `CREATE EXTENSION IF NOT EXISTS unaccent;`
- Servicios: `GET /services/wellbeing` cubre las categorías PSICOLOGIA, SALUD, DEPORTE, CULTURA, PASTORAL y BECAS. `GET /services/departments` cubre las filas con categoría `DEPARTAMENTO`. El `{type}` de `/admin/services/{type}` es `wellbeing`, `departments` o `faq`.
- En desarrollo no se envían correos: el token de recuperación de contraseña se imprime en el log de la aplicación.

## Decisiones respecto al enunciado

- **Tabla `token_refresco`** (añadida al esquema). El enunciado exige guardar el refresh token en BD e invalidarlo en el logout, pero el esquema base no tenía dónde. Solo se guarda el hash SHA-256 del token.
- **Estado `ARCHIVADO` en `noticia`** (añadido al `CHECK`). La eliminación lógica de noticias necesita un estado; `BORRADOR` y `PUBLICADO` no sirven para eso. Nunca aparece en consultas.
- **Bloqueo de cuenta**: al llegar a 5 intentos fallidos se fija `bloqueado_hasta` y el contador vuelve a 0, para que el bloqueo funcione también después de vencer el primero.
- **Eliminación definitiva (Ley 1581)**: borra al usuario, sus tokens y matrículas. Las noticias y eventos que creó pasan al administrador que ejecuta la acción, y la auditoría del usuario se anonimiza. El registro de auditoría de la eliminación no guarda datos personales.
- **Matrículas**: el horario del estudiante se lee de la tabla `matricula`, pero el enunciado no define un endpoint para crearlas. Por ahora se cargan por SQL o desde un endpoint que se agregue después.
