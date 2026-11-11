# Preguntas — Requirements Analysis

> Requisitos del trabajo actual: el `compose.yaml` raíz para Podman Compose y los
> scripts SQL de inicialización. Todo lo que ya está decidido en `AGENTS.md`,
> `ARCHITECTURE.md` y `CHALLENGE.md` se toma como fuente y no se vuelve a
> preguntar; estas preguntas cubren lo que esas fuentes dejan abierto para este
> entregable.

## Q1. [arquitectura] ¿Qué servicios entran en este `compose.yaml`?

La arquitectura nombra servicios de aplicación (order, appointment, notification-worker, gateway, frontend, Keycloak), pero hoy no existe ningún `Dockerfile` y los contextos de build apuntan a carpetas que no contienen el `pom.xml`. Si el compose los declara ahora, `podman compose up --build` falla.

- A. Solo la infraestructura de datos: dos PostgreSQL 16 y RabbitMQ con management; los servicios de aplicación entran cuando existan sus imágenes
- B. Infraestructura más los servicios de aplicación, con `build:` y `Dockerfile` creados en este mismo trabajo
- C. Infraestructura más los servicios, pero con los de aplicación comentados hasta que haya imágenes
- D. Infraestructura más gateway, frontend y Keycloak por imágenes públicas, sin los servicios Java
- X. Other (please specify)

[Answer]: B, ampliado — el compose debe contemplar todo el stack (frontend, servicios y bases de datos) para que un solo `podman compose up` lo levante y baste con abrirlo en el navegador; eso incluye crear los `Dockerfile` que hoy no existen. — (**Mode:** chat, 2026-11-11T13:00:56Z)

## Q2. [arquitectura] ¿Dos contenedores de PostgreSQL o uno con dos bases?

`AGENTS.md` pide una base por servicio; no dice si cada base vive en su propio contenedor. Dos contenedores aíslan mejor y permiten credenciales independientes; uno solo con dos bases por script es más liviano.

- A. Dos contenedores (`orders-db` y `appointments-db`), cada uno con su base, su usuario y su volumen
- B. Un contenedor con dos bases y dos usuarios creados por script
- C. Dos contenedores, pero con un único volumen compartido
- X. Other (please specify)

[Answer]: B. Un contenedor con dos bases y dos usuarios creados por script, para que el stack sea más liviano. — (**Mode:** chat, 2026-11-11T13:00:56Z)

## Q3. [datos] ¿Qué extensiones deben quedar creadas en cada base?

`ARCHITECTURE.md` menciona identificadores UUID y búsquedas; `AGENTS.md` no fija extensiones. Lo que se cree acá queda para siempre en el volumen de datos.

- A. `uuid-ossp` y `pgcrypto` en ambas bases, nada más
- B. `uuid-ossp`, `pgcrypto` y `pg_trgm` en ambas
- C. Solo `pgcrypto`, y los UUID se generan en la aplicación
- D. Ninguna: la aplicación maneja los identificadores y las migraciones Flyway crean lo que necesiten
- X. Other (please specify)

[Answer]: D, con matiz — los UUID los genera el backend y las extensiones las crea Flyway según lo que necesite cada entidad; los scripts de inicialización no fijan extensiones de antemano. — (**Mode:** chat, 2026-11-11T13:00:56Z)

## Q4. [datos] ¿Los scripts de inicialización crean solo bases y usuarios, o también el esquema?

En `docker-entrypoint-initdb.d` se puede crear desde la base vacía hasta tablas, triggers y datos de ejemplo. La arquitectura dice que Flyway gestiona el esquema de cada servicio.

- A. Solo bases, usuarios, permisos y extensiones; el esquema lo crean las migraciones Flyway de cada servicio
- B. Bases, usuarios, extensiones y además el esquema inicial (tablas y triggers) en SQL
- C. Bases, usuarios, extensiones y datos de ejemplo para poder demostrar el flujo
- X. Other (please specify)

[Answer]: B + C — los scripts inicializan las bases, el esquema y unos usuarios de prueba. — (**Mode:** chat, 2026-11-11T13:00:56Z)

## Q5. [datos] ¿Qué pasa si el volumen ya existe con datos de una versión anterior del compose?

Los scripts de `docker-entrypoint-initdb.d` solo corren cuando el volumen está vacío. Al reescribir el archivo con usuarios por base, un volumen viejo creado con el usuario único `postgres` queda inconsistente.

- A. Se documenta que hay que borrar el volumen (`podman volume rm`) y se asume pérdida de datos locales
- B. Se agrega un script idempotente que corrija usuarios y permisos si el volumen ya existe
- C. Se usa un nombre de volumen nuevo para el entorno corregido y el viejo se deja intacto
- X. Other (please specify)

[Answer]: C. Se usa un nombre de volumen nuevo para el entorno corregido y el volumen viejo se deja intacto. — (**Mode:** chat, 2026-11-11T13:00:56Z)

## Q6. [seguridad] ¿Cómo se manejan las credenciales locales?

Hoy el archivo trae `postgres`/`postgrespassword` y RabbitMQ con `guest`/`guest`. La arquitectura exige secretos por entorno y no versionar credenciales reales.

- A. Valores por defecto en el `compose.yaml` para desarrollo local, sobreescribibles por variables de entorno, y un `.env.example` documentado
- B. Todo por variables de entorno obligatorias, sin valores por defecto (el stack no arranca sin `.env`)
- C. Un archivo `.env` versionado con valores desechables del taller y documentado como tal
- X. Other (please specify)

[Answer]: A. Valores por defecto locales en el `compose.yaml`, sobreescribibles por variables de entorno, más un `.env.example` documentado. — (**Mode:** chat, 2026-11-11T13:00:56Z)

## Q7. [operaciones] ¿Qué garantías de arranque y salud debe dar el compose?

Hoy hay `depends_on` sin `healthcheck`, así que los servicios pueden arrancar antes de que las bases acepten conexiones. La práctica del equipo exige verificar de punta a punta antes de cerrar la entrega.

- A. `healthcheck` en cada contenedor (`pg_isready`, `rabbitmq-diagnostics`), `depends_on` con `condition: service_healthy`, política de reinicio `unless-stopped` y volúmenes nombrados
- B. Lo mismo que A, más límites de CPU y memoria por servicio
- C. Solo `healthcheck`, sin política de reinicio ni límites
- X. Other (please specify)

[Answer]: B. `healthcheck` en cada contenedor, `depends_on` con `condition: service_healthy`, reinicio `unless-stopped`, volúmenes nombrados y límites de CPU y memoria. — (**Mode:** chat, 2026-11-11T13:00:56Z)

## Q8. [operaciones] ¿RabbitMQ necesita algo más que management habilitado?

La arquitectura lo usa para eventos de dominio con outbox transaccional. El acceso por defecto `guest`/`guest` solo funciona desde `localhost`, lo que rompe el modelo de contenedores en red privada.

- A. Usuario y contraseña propios, un vhost dedicado para el taller, management en 15672 y AMQP 5672 solo dentro de `workshop-net`
- B. Usuario y contraseña propios, vhost por defecto `/`
- C. `guest`/`guest` con la variable que permite acceso remoto, solo para el taller
- X. Other (please specify)

[Answer]: A. Usuario y contraseña propios, vhost dedicado, management en 15672 y AMQP 5672 solo dentro de `workshop-net`. — (**Mode:** chat, 2026-11-11T13:00:56Z)

## Follow-Up Questions

> Estas preguntas cierran contradicciones que aparecieron entre las respuestas
> anteriores; no se pueden generar requisitos coherentes sin resolverlas.

## F1. [arquitectura] El contenedor único de PostgreSQL frente a la regla de "nunca compartir una base entre servicios"

La respuesta a la pregunta 2 (un solo contenedor con dos bases) roza una regla ya afirmada para este proyecto: "nunca compartir una única base de datos relacional entre servicios, ni siquiera como atajo de desarrollo". Mi lectura es que la regla se respeta si cada servicio sigue siendo dueño de su propia base —dos bases separadas, dos roles sin acceso cruzado y ninguna consulta entre dominios— y lo único compartido es el servidor PostgreSQL. ¿Es correcta esa lectura o prefieres volver a dos contenedores?

- A. Sí: un solo servidor PostgreSQL con dos bases independientes y dos roles sin acceso cruzado; la regla se cumple porque ningún servicio lee la base del otro
- B. No: dos contenedores separados (`orders-db` y `appointments-db`), cada uno con su base, su rol y su volumen
- C. Un contenedor, pero con un esquema por servicio dentro de una misma base
- X. Other (please specify)

[Answer]: A. Un solo servidor PostgreSQL con dos bases independientes y dos roles sin acceso cruzado; ningún servicio lee la base del otro, así que la regla se cumple. — (**Mode:** chat, 2026-11-11T13:09:23Z)

## F2. [datos] ¿Quién es dueño del esquema: los scripts de inicialización o Flyway?

En la pregunta 3 dijiste que Flyway crea lo que haga falta, y en la 4 que los scripts de inicialización crean el esquema. Las dos cosas no pueden ser dueñas de las mismas tablas sin duplicarlas. ¿Dónde queda el límite?

- A. Los scripts crean bases, roles y permisos; el esquema lo crean las migraciones Flyway de cada servicio y los scripts no crean tablas
- B. Los scripts crean bases, roles y el esquema completo (tablas y triggers); Flyway queda para cambios posteriores sobre ese esquema base
- C. Los scripts crean bases y roles, y el esquema inicial vive dentro de cada servicio como su primera migración Flyway (`V1`)
- X. Other (please specify)

[Answer]: A. Los scripts crean bases, roles y permisos; el esquema lo crean las migraciones Flyway de cada servicio y los scripts no crean tablas. — (**Mode:** chat, 2026-11-11T13:09:23Z)

## F3. [datos] ¿Qué son los "usuarios de prueba" de la pregunta 4?

Necesito saber si son usuarios de la base de datos o personas que inician sesión en el sistema, porque en este punto del trabajo todavía no hay autenticación ni Keycloak levantado.

- A. Usuarios de base de datos: los roles `orders_user` y `appointment_user`, con permisos solo sobre su propia base
- B. Personas de prueba para iniciar sesión en el sistema (por ejemplo un asesor y un mecánico), sembradas como datos en el esquema
- C. Ambos: los roles de base y además personas de prueba sembradas
- X. Other (please specify)

[Answer]: Cuando estén listos la autorización y la autenticación, dejar usuarios ya creados para poder entrar y probar; en este trabajo los scripts solo crean los roles de base y los usuarios de aplicación llegan con el bloque de autenticación. — (**Mode:** chat, 2026-11-11T13:09:23Z)

## F4. [arquitectura] ¿Qué se levanta en el puerto 8081 si todavía no existe el gateway?

`ARCHITECTURE.md` pone un gateway en el puerto 8081 como única entrada al backend, pero en el repositorio no hay ningún proyecto de gateway: solo existen `order-service`, `appointment-service` y `notification-worker`. Para que el frontend hable con el backend hay que decidir qué componente atiende ese puerto.

- A. Crear en este trabajo un gateway mínimo (Spring Cloud Gateway) que enrute hacia `order-service` y publique el 8081
- B. Dejar el gateway fuera por ahora: el frontend llama al `order-service` por la red interna y el 8081 queda pendiente para la siguiente rebanada, registrado como deuda
- C. Publicar el `order-service` directamente en 8081 y llamarlo "gateway" mientras no exista el componente real
- X. Other (please specify)

[Answer]: A. Crear en este trabajo un gateway mínimo (Spring Cloud Gateway) que enrute hacia `order-service` y publique el 8081. — (**Mode:** chat, 2026-11-11T13:09:23Z)

## F5. [producto] ¿Qué debe verse en el navegador para considerar que "funciona"?

Los servicios son esqueletos sin endpoints de negocio, así que "abrir en el navegador y que funcione" necesita un criterio concreto y verificable.

- A. Una página del frontend que consulte el estado del backend y muestre que la conexión front → gateway/back → base responde
- B. El frontend sirviendo su pantalla inicial, y la conexión con el backend se verifica aparte con una llamada HTTP desde la terminal
- C. Una página con el listado de órdenes leídas de `orders_db`, aunque sea con datos de ejemplo
- X. Other (please specify)

[Answer]: A ampliado — entrar a la página del frontend, registrar órdenes, verlas en un listado y completar un mini flujo; no basta con un chequeo de estado. — (**Mode:** chat, 2026-11-11T13:09:23Z)

## Consolidated Summary Confirmation

- **Q1 — Alcance del compose:** todo el stack queda declarado —frontend, gateway, los tres servicios y las bases— para que un solo `podman compose up` lo levante; se crean los `Dockerfile` que hoy no existen. El entregable crece: ya no es solo `compose.yaml` y scripts, también las imágenes.
- **Q2 + F1 — PostgreSQL:** un solo servidor con dos bases independientes (`orders_db` y `appointments_db`) y dos roles sin acceso cruzado. La regla de no compartir base se cumple porque ningún servicio lee la base del otro.
- **Q3 + F2 — Dueño del esquema:** los UUID los genera el backend; las extensiones las crea Flyway si alguna entidad las necesita; los scripts de inicialización crean bases, roles y permisos, y el esquema lo crean las migraciones Flyway, no los scripts.
- **Q4, corregida por F2 y F3:** los scripts dejan las bases y los roles listos; no crean tablas ni usuarios de aplicación.
- **F3 — Usuarios de prueba:** los usuarios para entrar y probar el sistema se dejan creados cuando exista el bloque de autenticación y autorización; en este trabajo solo se crean los roles de base.
- **Q5 — Volumen existente:** se usa un nombre de volumen nuevo para el entorno corregido y el volumen viejo queda intacto.
- **Q6 — Credenciales locales:** valores por defecto en el `compose.yaml`, sobreescribibles por variables de entorno, más un `.env.example` documentado.
- **Q7 — Salud y arranque:** `healthcheck` en cada contenedor, `depends_on` con `condition: service_healthy`, reinicio `unless-stopped`, volúmenes nombrados y límites de CPU y memoria.
- **Q8 — RabbitMQ:** usuario y contraseña propios, vhost dedicado, management en 15672 y AMQP 5672 solo dentro de `workshop-net`.
- **F4 — Gateway:** se crea un gateway mínimo (Spring Cloud Gateway) que publica el 8081 y enruta hacia `order-service`.
- **F5 — Criterio de éxito en el navegador:** entrar a la página del frontend, registrar órdenes, verlas en un listado y abrir su detalle; cada orden creada deja su registro inicial en el historial append-only. El ciclo completo de estados, las citas, el WhatsApp y la auditoría completa llegan en rebanadas posteriores.

Does this all look correct before I generate the requirements artifact?

- Looks correct
- Request changes

[Answer]: Looks correct
