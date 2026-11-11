# Requisitos de seguridad — Stack Podman y rebanada de órdenes

> Etapa NFR Requirements. Deriva de `requirements.md` (NFR2, NFR4, NFR7) y de las
> respuestas confirmadas en `nfr-requirements-questions.md`. Contexto de referencia:
> `requirements-analysis/requirements.md`, `AGENTS.md` §2 y §4, `ARCHITECTURE.md`
> (TAC-005, PAT-001, PAT-004).
> En esta rebanada la persona pidió "lo más indispensable": controles que el flujo
> necesita para funcionar y que el despliegue local se sostenga, sin autenticación
> todavía.

## Alcance

Cubre el aislamiento de datos entre dominios, la inmutabilidad del historial de
estados, el manejo de credenciales y los controles de entrada en el borde. La
autenticación y la autorización por rol quedan fuera y llegan con el bloque de
identidad.

## Requisitos

| ID | Requisito | Criterio de verificación |
|----|-----------|--------------------------|
| **NFR2.1** | `orders_user` no puede leer ni escribir en `appointments_db`, y `appointment_user` no puede leer ni escribir en `orders_db`. | Prueba automatizada de integración que intenta conectar con `psql` usando las credenciales de `orders_user` contra `appointments_db` y de `appointment_user` contra `orders_db`, y exige error de permisos en ambos casos. La prueba nunca usa el superusuario `postgres`. |
| **NFR2.2** | Cada servicio se conecta exclusivamente a su propia base usando su propio rol, con credenciales vindas de variables de entorno. | Configuración de conexión de `order-service` y `appointment-service` con usuario y base distintos; `information_schema` no muestra tablas de la otra base para cada rol. |
| **NFR2.3** | Ningún puerto de datos se publica al host: 5432 (PostgreSQL), 5672 (AMQP) y los puertos internos de servicio (8082/8083) quedan dentro de `workshop-net`. | `podman compose ps` solo muestra publicados 8080, 8081 y 15672; el gateway es la única entrada al backend. |
| **NFR4.1** | `order_status_history` es append-only: un trigger de base de datos y los permisos del rol rechazan `UPDATE` y `DELETE` sobre la tabla. | Prueba que intenta `UPDATE` y `DELETE` y recibe un error; el rol de aplicación solo tiene `SELECT` e `INSERT` sobre la tabla. |
| **NFR4.2** | El `eventHash` se calcula con SHA-256 en hexadecimal minúsculas sobre una serialización canónica inequívoca: para cada uno de los ocho campos, en el orden fijo `eventId`, `orderId`, `fromStatus`, `toStatus`, `reason`, `changedBy`, `changedAt`, `previousHash`, se escribe `<longitudEnBytes>:<valor>` en UTF-8 y los ocho fragmentos se unen con `|`. Un valor nulo se escribe `~` (sin longitud) y un valor vacío `0:`. Los UUID van en su forma textual RFC 4122 en minúsculas y `changedAt` en ISO-8601 UTC con milisegundos (`yyyy-MM-dd'T'HH:mm:ss.SSS'Z'`). | Un verificador recalcula el hash de cada fila a partir de las columnas almacenadas aplicando estas mismas reglas y coincide con el `eventHash` guardado; el formato está documentado en el repositorio. |
| **NFR4.3** | La cadena arranca con `previousHash` igual a 64 ceros en la primera fila de cada orden y cada fila siguiente encadena el `eventHash` de la fila anterior de esa misma orden. | La primera fila del historial de cada orden tiene `previousHash` de 64 ceros y su hash se recalcula y coincide. El recorrido completo de una cadena de varias filas se verifica en la rebanada del ciclo de estados (ver «Diferido»), que es la que produce una segunda fila por orden. |
| **NFR4.4** | `changedBy` y `changedAt` (UTC) son obligatorios en toda fila del historial y no admiten valores nulos ni edición posterior. | Restricción `NOT NULL` en el esquema y rechazo de cualquier intento de modificación (ver NFR4.1). |
| **NFR7.1** | Ningún secreto real se versiona ni se incrusta en una imagen: credenciales por variables de entorno y un `.env.example` con valores de ejemplo. | `git grep` de contraseñas no encuentra valores reales; las imágenes se construyen sin credenciales incrustadas. |
| **NFR7.2** | Validación de entrada en el borde y en el servicio, con longitudes máximas fijas: razón social del cliente 120 caracteres, descripción del trabajo 2.000, matrícula 16, teléfono 32, correo 120, código de aseguradora 32 e identificadores 36 (UUID). Los campos obligatorios no admiten vacío y las respuestas de error devuelven 4xx sin trazas internas. | Un caso de prueba por regla de validación, incluidos los casos de longitud máxima y de exceso de longitud; las columnas de la migración Flyway respetan los mismos límites y la respuesta de error no contiene nombre de clase ni traza de pila. |
| **NFR7.3** | CORS restringido al origen del frontend (`http://localhost:8080`); sin comodines ni credenciales cross-origin. | El gateway rechaza un `Origin` distinto del permitido; la llamada desde el navegador funciona y una petición con `Origin` ajeno no. |
| **NFR7.4** | RabbitMQ no acepta `guest`/`guest`: usa un usuario y contraseña propios con el tag `management` y permisos de lectura y escritura sobre el vhost dedicado, sin el tag `administrator`. | `rabbitmqctl list_users` muestra el usuario propio con el tag `management` y `guest` no autentica por AMQP; la interfaz de management responde con ese usuario y muestra la cola y los consumidores del stack. |
| **NFR7.5** | En esta rebanada el gateway no expone endpoints administrativos ni de gestión de datos; solo enruta la API de órdenes. | El gateway publica únicamente las rutas de `/api/orders/**` y su endpoint de salud. |

## Notas de diseño

- **La integridad se apoya en la base, no en la aplicación.** El trigger y los
  permisos del rol protegen el historial aunque el código de aplicación tuviera un
  error: una escritura directa con `psql` también falla. Es la lectura que fija
  `AGENTS.md` §2.B.3 y `ARCHITECTURE.md` PAT-004.
- **El hash se define ahora y no después.** Como la tabla es append-only, corregir
  el formato del hash más adelante obligaría a rehacer el historial ya escrito. Fijar
  SHA-256, el orden de campos, la codificación con prefijo de longitud y el génesis
  de 64 ceros en este momento evita ese costo (cierra el hallazgo R-01 de la revisión
  de requisitos y el R-02 de la revisión de esta etapa).
- **CORS no es opcional en esta rebanada.** El frontend se sirve en 8080 y el gateway
  escucha en 8081: sin CORS restringido al origen del frontend, el navegador bloquea
  la llamada y el flujo de FR7.7 no funciona.
- **Autenticación diferida, no olvidada.** La ausencia de login es una decisión
  consciente del MVP; el bloque de identidad traerá OIDC/JWT, RBAC y el `changedBy`
  tomado del token, tal como exige `AGENTS.md` §2.B.2.

## Diferido

| ID | Motivo | Cuándo se retoma |
|----|--------|------------------|
| Autenticación y RBAC | El bloque de autenticación y autorización todavía no existe; `ARCHITECTURE.md` propone Keycloak en 8180 y el gateway debe filtrar tokens. Sin login, `changedBy` se registra con un identificador de desarrollo fijo y documentado (supuesto S5 de `requirements.md`). | Rebanada de identidad, con usuarios de prueba creados para entrar al sistema; allí el puerto 8180 de Keycloak se incorpora a la lista de puertos internos. |
| Rate limiting en el gateway | No hay exposición a red externa: el stack es local y de un taller, con un solo usuario previsto. | Cuando exista un ambiente compartido o exposición fuera de la red local. |
| Verificación de una cadena de varias filas (NFR4.3) y recorrido completo del historial (NFR4.6) | En esta rebanada cada orden tiene una sola fila de historial, porque el ciclo de estados está fuera de alcance; una cadena de varias filas no se puede producir ni verificar. | Rebanada del ciclo de estados, que genera la segunda fila por orden y hace verificable el encadenamiento completo. |
