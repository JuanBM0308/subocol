# Requisitos — Stack Podman Compose y primera rebanada de órdenes

## Análisis de la intención

El objetivo no es "escribir un `compose.yaml`": es que cualquier persona del taller
pueda clonar el repositorio, ejecutar **un solo comando** (`podman compose up --build`)
y encontrarse con el sistema funcionando en el navegador, sin pasos manuales, sin
credenciales a mano y sin depender de Docker Desktop. Sobre esa base se quiere ver
la costura funcionando de punta a punta: registrar una orden desde el frontend,
verla listada y abrir su detalle, atravesando frontend → gateway → `order-service`
→ `orders_db` con datos reales.

Ese segundo objetivo es una **rebanada vertical** (walking skeleton): no implementa
el ciclo completo de la orden, pero prueba hoy que las piezas encajan, que el
aislamiento por base se sostiene y que el historial append-only existe desde el
primer registro. Las funcionalidades restantes del taller (citas, cupos, WhatsApp,
reapertura, autenticación) se montan después sobre esta base.

El trabajo respeta la arquitectura documentada en `ARCHITECTURE.md` y las reglas
afirmadas del equipo en `team-practices.md`; la fuente de lo que se le debe al
cliente es `CHALLENGE.md`. `DECISIONES.md` es del desarrollador y no se usa como
fuente para el agente.

## Fuentes

- `CHALLENGE.md` — problema del taller y entregables comprometidos con el cliente.
- `AGENTS.md` — stack obligatorio, fronteras de dominio, puertos y reglas de auditoría.
- `ARCHITECTURE.md` — arquitectura TO-BE, puertos de referencia, TAC/PAT/RST y la
  nota de que `orders_db` y `appointments_db` pueden convivir en una instancia
  PostgreSQL durante el desarrollo.
- `team-practices.md` — prácticas afirmadas: prueba después de cada capa, piso de
  cobertura, despliegue local con Podman, rebanada vertical siempre.
- `requirements-analysis-questions.md` — respuestas Q1–Q8 y F1–F5 de esta etapa.

## Requisitos funcionales

### FR1 — Orquestación del stack completo con un solo comando

- **FR1.1** El `compose.yaml` raíz declara el frontend, el gateway, `order-service`,
  `appointment-service`, `notification-worker`, un PostgreSQL 16 y RabbitMQ.
- **FR1.2** `podman compose up --build` levanta todo el stack sin pasos manuales
  previos ni comandos auxiliares.
- **FR1.3** Con el stack arriba, `http://localhost:8080` sirve el frontend.
- **FR1.4** Todos los contenedores se conectan a la red privada `workshop-net`.
- **FR1.5** Solo se publican los puertos 8080 (frontend), 8081 (gateway) y 15672
  (management de RabbitMQ); PostgreSQL 5432, AMQP 5672, Keycloak 8180 y los puertos
  internos de los servicios (8082/8083) quedan sin publicar.
- **FR1.6** `podman compose down` detiene el stack sin borrar los volúmenes de datos.

### FR2 — Aislamiento de datos en una sola instancia PostgreSQL

- **FR2.1** Un único contenedor PostgreSQL 16 aloja `orders_db` y `appointments_db`
  como bases independientes.
- **FR2.2** Existen los roles `orders_user` y `appointment_user`, cada uno con
  permisos solo sobre su propia base y sin acceso a la otra.
- **FR2.3** Ningún servicio abre conexiones ni ejecuta consultas contra la base del
  otro dominio.
- **FR2.4** El volumen de datos usa un nombre nuevo, distinto del que usaba el
  `compose.yaml` anterior, y el volumen viejo queda intacto.

### FR3 — Inicialización de las bases

- **FR3.1** Los scripts de `docker-entrypoint-initdb.d` crean las dos bases y los dos
  roles con sus permisos.
- **FR3.2** Los scripts de inicialización no crean tablas, triggers ni extensiones.
- **FR3.3** Las credenciales de PostgreSQL y de RabbitMQ se leen de variables de
  entorno, con valores por defecto para desarrollo local.
- **FR3.4** Un `.env.example` versionado documenta todas las variables y no contiene
  secretos reales.

### FR4 — Esquema, migraciones e historial

- **FR4.1** Cada servicio aplica el esquema de su base con migraciones Flyway al
  arrancar.
- **FR4.2** Una extensión de PostgreSQL se crea desde una migración solo si alguna
  entidad la necesita.
- **FR4.3** Los identificadores UUID se generan en el backend, no en la base.
- **FR4.4** `order_status_history` se crea por migración como tabla append-only, con
  un trigger que rechaza `UPDATE` y `DELETE`.
- **FR4.5** Cada fila de `order_status_history` guarda `eventId`, `orderId`,
  `fromStatus`, `toStatus`, `reason`, `changedBy`, `changedAt` en UTC y `eventHash`
  encadenado al `previousHash`.

### FR5 — Imágenes y build

- **FR5.1** Cada servicio Java tiene su `Dockerfile` multi-etapa sobre Java 25 y
  Maven Wrapper.
- **FR5.2** El frontend tiene su `Dockerfile` multi-etapa que compila Angular y sirve
  el build de producción.
- **FR5.3** `podman compose up --build` construye todas las imágenes sin utilidades
  exclusivas de Docker Desktop.
- **FR5.4** Ninguna imagen contiene contraseñas, claves ni secretos.

### FR6 — Gateway de entrada

- **FR6.1** Existe un módulo de gateway con Spring Cloud Gateway que escucha en el
  puerto 8081.
- **FR6.2** El gateway enruta `/api/orders/**` hacia `order-service`.
- **FR6.3** El frontend solo conoce el puerto 8081 para hablar con el backend; no
  conoce los puertos internos de los servicios.
- **FR6.4** El gateway expone un endpoint de salud que alimenta su `healthcheck`.

### FR7 — Mini flujo de órdenes

- **FR7.1** La pantalla del frontend permite registrar una orden con cliente,
  vehículo, descripción del trabajo y aseguradora opcional.
- **FR7.2** El formulario valida en cliente y en servidor los campos obligatorios y
  muestra mensajes de error accionables.
- **FR7.3** La orden se persiste en `orders_db` en estado `RECIBIDA` al crearse.
- **FR7.4** La creación de la orden y la inserción de su primera fila en
  `order_status_history` ocurren en una sola transacción.
- **FR7.5** El frontend lista las órdenes registradas con sus datos mínimos y su
  estado actual.
- **FR7.6** El frontend muestra el detalle de una orden, incluido su historial de
  estados.
- **FR7.7** El flujo registrar → listar → ver detalle se completa desde el navegador
  sin SQL manual ni pasos por terminal.
- **FR7.8** Si el backend no responde, el frontend muestra un error visible en lugar
  de una pantalla vacía.

### FR8 — Operación y salud

- **FR8.1** Cada contenedor define un `healthcheck` propio.
- **FR8.2** Cada servicio declara `depends_on` con `condition: service_healthy`
  sobre las dependencias que necesita.
- **FR8.3** Los contenedores usan política de reinicio `unless-stopped`.
- **FR8.4** Los datos persisten en volúmenes nombrados.
- **FR8.5** Cada servicio define límites de CPU y memoria.
- **FR8.6** RabbitMQ usa usuario y contraseña propios, un vhost dedicado y
  management publicado en 15672.

## Requisitos no funcionales

- **NFR1 — Reproducibilidad:** desde un clon limpio con Podman instalado,
  `podman compose up --build` deja todos los contenedores saludables y el flujo
  usable en 10 minutos o menos.
- **NFR2 — Aislamiento verificable:** una prueba automática demuestra que
  `orders_user` no puede leer `appointments_db` y que `appointment_user` no puede
  leer `orders_db`.
- **NFR3 — Cobertura:** 80 % de líneas o más por componente, medido con JaCoCo en el
  backend y con el umbral de Vitest en el frontend; el build falla por debajo.
- **NFR4 — Integridad del historial:** una prueba demuestra que `UPDATE` y `DELETE`
  sobre `order_status_history` fallan y que la cadena de hash detecta una alteración.
- **NFR5 — Observabilidad:** cada servicio emite logs estructurados con
  `correlationId` y expone su estado de salud.
- **NFR6 — Portabilidad:** el stack funciona con `podman compose` y con
  `podman-compose`, sin características exclusivas de Docker Desktop.
- **NFR7 — Seguridad local:** no se versionan secretos reales, no se usa
  `guest`/`guest` y ningún puerto de datos se publica.
- **NFR8 — Rendimiento:** registrar una orden y listar hasta 1.000 órdenes responden
  en menos de 1 segundo en el equipo del taller.
- **NFR9 — Usabilidad:** el flujo registrar → listar → detalle se completa en cinco
  pasos o menos desde la pantalla inicial.

## Restricciones

- Java 25 con Spring Boot 4.1.2 en los servicios; Angular 22 con componentes
  standalone, señales y formularios reactivos en el frontend.
- PostgreSQL 16 y RabbitMQ como única persistencia y único broker.
- Orquestación exclusivamente con Podman y Podman Compose sobre `compose.yaml` OCI
  portable.
- Solo 8080, 8081 y 15672 se publican; el resto vive en `workshop-net`.
- Un usuario de base por base, con permisos solo sobre la suya.
- El esquema pertenece a Flyway; los scripts de inicialización solo crean bases,
  roles y permisos.
- Los UUID se generan en el backend.
- En esta rebanada no hay autenticación ni Keycloak, y no se siembran usuarios de
  aplicación; eso llega con el bloque de autenticación.
- El volumen viejo del compose anterior no se borra ni se reutiliza.
- `DECISIONES.md` es propiedad del desarrollador y no es fuente de requisitos para
  el agente.
- El taller tiene un límite de un día, lo que acota el tamaño de la rebanada.

## Supuestos

- **S1 — RabbitMQ arranca vacío:** los servicios declaran las colas y exchanges que
  necesiten al arrancar; no se exige un archivo de definiciones previo. *Razón:* en
  esta rebanada no hay publicación de eventos todavía.
- **S2 — Frontend sin SSR:** se sirve el build de producción como sitio estático; el
  renderizado en servidor no es requisito de esta rebanada.
- **S3 — Alcance del mini flujo:** "registrar, ver y mini flujo" se interpreta como
  crear la orden, listarla y abrir su detalle con el historial inicial. Las
  transiciones posteriores de estado llegan en la siguiente rebanada.
- **S4 — Estado inicial:** se usa `RECIBIDA`, del catálogo propuesto en
  `ARCHITECTURE.md`, que sigue sujeto a validación funcional (RST-08).
- **S5 — Actor sin autenticación:** mientras no exista JWT, `changedBy` se registra
  con un identificador de desarrollo fijo y documentado; sustituirlo por el sujeto
  del token es parte del bloque de autenticación.
- **S6 — Entorno del taller:** el equipo donde se ejecuta tiene Podman con el
  provider de compose disponible y acceso a registros públicos de imágenes.
- **S7 — Esqueletos existentes:** `order-service`, `appointment-service` y
  `notification-worker` son esqueletos de Spring Boot; este trabajo agrega los
  endpoints mínimos que la rebanada necesita.
- **S8 — Rama de trabajo:** se trabaja sobre `feature/realiease/1-architecture`;
  todavía no hay un `main` estable y los commits van en inglés.

## Fuera de alcance

- Autenticación y autorización, Keycloak, roles de administrador y mecánico, y
  usuarios de aplicación sembrados.
- Citas, franjas horarias, cupos por asesor y control de concurrencia.
- El worker de notificaciones más allá de estar arriba y saludable; el log simulado
  de WhatsApp.
- La máquina de estados completa, el cierre de orden, la reapertura con motivo, el
  evento `OrderClosed` y el Transactional Outbox.
- La consulta de auditoría en la interfaz y el almacenamiento WORM.
- Pipeline de CI/CD, staging y producción.
- Datos reales de clientes, vehículos o aseguradoras.
- Las pantallas y reglas de negocio restantes del taller.

## Preguntas abiertas

- **O1 — Catálogo definitivo de estados:** los nombres y transiciones siguen
  propuestos en `ARCHITECTURE.md` (RST-08) y necesitan validación funcional antes de
  implementar el ciclo completo.
- **O2 — Estado inicial oficial:** si el taller prefiere otro nombre distinto de
  `RECIBIDA`, cambia el valor sembrado en la primera fila del historial.
- **O3 — Reglas de agenda:** duración de franjas, cupos máximos y solapamiento
  quedan para la rebanada de citas.
- **O4 — Rol autorizado para reabrir:** el rol `SUPERVISOR` está enunciado en
  `AGENTS.md`, pero su equivalencia con usuarios reales se define en el bloque de
  autenticación.
- **O5 — Volumen viejo:** si alguien necesita reutilizar el nombre anterior, deberá
  borrarlo a mano; el compose no lo toca.
