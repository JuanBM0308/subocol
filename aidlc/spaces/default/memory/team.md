# Team-Level Rules

> This team's affirmed practices and corrections. Loaded after `org.md` as
> strict-additive guidance; contradictions with broader policy are rejected.
> Populated by the practices-discovery affirmation gate. Edit at the gate,
> not directly.

## Way of Working

Todavía **no hay un `main` estable**. La única rama publicada es
`feature/realiease/1-architecture`, el historial tiene dos commits y no hay
ninguna integración observada. Por eso la política de integración queda como
pendiente explícito: no la damos por decidida hasta que se fije antes de la
primera integración real.

- Trabajamos con **ramas cortas por tema**, publicadas al remoto, y la rama es el
  registro de lo que se está cambiando. `ARCHITECTURE.md` (AMB-01) pide crear
  ramas por historia y correr pruebas unitarias antes de integrar, y eso es lo
  que hacemos.
- El patrón de nombre es `feature/<tema>/<alcance>` (por ejemplo
  `feature/compose-stack/1-red-workshop`). La rama vigente trae un error de
  tipeo (`realease`), señal de que la convención no está verificada por
  automatización: se corrige al crear la siguiente, no se renombra el historial.
- **Los commits van en inglés, en orden normal**: mensajes cronológicos, sin
  reescritura de historia y sin exigir prefijos tipo Conventional Commits
  mientras no haya automatización que los consuma. La descripción del cambio va
  en una línea de resumen y, cuando hace falta contexto, un cuerpo breve con el
  porqué.
- Integración: **squash merge frente a merge commit sigue sin decidirse**. Es un
  pendiente del equipo, no una práctica; cuando se fije, se registra acá y se
  aplica desde ese momento.
- Un cambio se integra cuando **la suite del componente afectado está en verde**,
  y la suite completa del componente también antes de cerrar la entrega. Nadie
  integra con pruebas rojas.
- El trabajo es unipersonal en este taller: la revisión por otra persona no es un
  requisito y no la simulamos. La rama y el mensaje de commit son la
  trazabilidad de quién cambió qué.
- Las decisiones de arquitectura se registran en `ARCHITECTURE.md`. Lo que se le
  debe al cliente se rige por `CHALLENGE.md`, que además exige entregar un
  `DECISIONES.md` con las fronteras trazadas, las alternativas descartadas y al
  menos dos propuestas de la IA rechazadas: ese documento lo mantiene el
  desarrollador y **no es fuente de reglas para el agente**.

## Walking Skeleton

**Sí, siempre.** Verificamos partes pequeñas de punta a punta a medida que se
implementan, antes de armar los bloques completos del sistema. Así no nos
llevamos sustos cuando las piezas grandes se ensamblan.

- Una rebanada es la versión mínima que corre de punta a punta: contenedor
  arriba, base propia conectada y un endpoint o un consumidor respondiendo con
  datos reales del camino, no un mock.
- Cada funcionalidad nueva arranca por su corte vertical más delgado. No es una
  ceremonia única de arranque: es cómo trabajamos, funcionalidad por
  funcionalidad.
- La verificación es explícita y ejecutable: `podman compose up --build` con los
  contenedores saludables y el servicio respondiendo dentro de `workshop-net`,
  más una prueba que cruce la frontera que se acaba de conectar.
- Hoy existen los esqueletos de front y back pero **no están conectados**. El
  primer corte pendiente es front → gateway → `order-service` → `orders_db`; el
  segundo es el camino de eventos con RabbitMQ y el worker de notificaciones.
- Si una rebanada no se puede verificar de punta a punta, el trabajo se detiene y
  se resuelve esa costura antes de agregar funcionalidad encima.

## Testing Posture

- **Methodology**: test-after
- **Ordering**: implementamos cada capa verificable y después escribimos y ejecutamos las pruebas de esa capa, cerrando con la suite completa del componente en verde antes de integrar.
- **Methodology evidence**: solo existen los esqueletos de front y back, con pruebas de contexto (`@SpringBootTest` con `contextLoads()`) y el spec de Angular del andamiaje; no hay ciclos rojo-verde ni documento que nombre TDD, BDD o ATDD. La entrevista confirmó test-after y la revisión capa por capa de lo que hay y lo que falta.
- **Cobertura**: piso obligatorio de **80 % de líneas por componente** (`AGENTS.md` §5), medido con **JaCoCo** en el backend y con el **umbral de cobertura de Vitest** en el frontend, y **el build falla por debajo**. Hoy ninguno de los dos está configurado: es la primera práctica a implementar, no una práctica vigente.
- **Herramientas backend**: JUnit 5 y Mockito sobre Maven Wrapper (`./mvnw test`) en cada servicio Spring Boot. Mockito todavía no está declarado en los `pom.xml`: se agrega junto con JaCoCo.
- **Herramientas frontend**: Vitest con jsdom a través del builder `@angular/build:unit-test` (`npm test`), con el umbral de cobertura fijado en la configuración de test.
- **Qué probamos además de la cobertura**: los invariantes que el taller considera críticos tienen prueba propia — concurrencia de cupos con reservas simultáneas, idempotencia de consumidores ante reentrega, historial append-only que rechaza modificación y supresión de la notificación cuando `notifyCustomer=false` (`AGENTS.md` §2; `ARCHITECTURE.md` TAC-003, TAC-004, PAT-004).
- **Pruebas de integración**: las que necesitan contenedores (PostgreSQL, RabbitMQ) corren aparte de la suite unitaria y no bloquean el ciclo rápido; se ejecutan cuando hay entorno levantado.
- **Datos de prueba**: sintéticos. Nunca datos reales de clientes, vehículos o aseguradoras en pruebas ni en logs.
- **El piso no se negocia**: no se baja el umbral ni se excluye código para que una entrega pase. Si un cambio baja la cobertura, se corrige o se justifica por escrito en la revisión.

## Guard Policy

<!-- Affirmed by the team. Mode: strict, relaxed, or off. Strict here holds for every intent, whatever one person asks in chat; relaxed or off here applies to every intent whose policy came from its scope, unless project.md or the person sets another; changing this line changes it. A section under the retired Change Control heading, written by an earlier release, is still read. -->

## Deployment

Desplegamos localmente con **Podman y Podman Compose**, y ese es hoy el único
entorno real del proyecto. Staging y producción son destinos, no entornos
existentes.

- `podman compose up --build` es el comando primario de despliegue y
  `podman compose down` el de baja. Si el provider no está disponible,
  `podman-compose up --build` es la alternativa documentada.
- El despliegue es **manual y bajo demanda**: quien integra un cambio lo levanta
  y lo verifica en su máquina antes de cerrar la entrega. No hay CD automático.
- Validación mínima antes de dar un despliegue por bueno: contenedores arriba y
  saludables, `orders_db` y `appointments_db` creadas con su propio usuario,
  RabbitMQ con management respondiendo y el frontend sirviendo.
- Todo componente —servicios, bases, broker y frontend— corre en contenedores OCI
  sobre la red privada `workshop-net`. Solo se publican los puertos de acceso
  humano: 8080 (frontend), 8081 (gateway) y 15672 (management de RabbitMQ). Las
  bases de datos no se publican.
- El `compose.yaml` **se reescribe desde cero** sobre el estándar y la
  arquitectura, conservando solo lo que ya sirve (imágenes y nombres de
  servicio). El vigente no cumple: usa `POSTGRES_MULTIPLE_DATABASES`, monta un
  `init-db.sql` que no existe, no declara red, publica 5432 y arrastra
  `version: '3.8'`.
- **Un usuario y una contraseña por base, con permisos solo sobre su propia
  base** (`AGENTS.md` §2.A; `ARCHITECTURE.md` TAC-005). El `postgres` compartido
  del archivo actual queda fuera. El acceso por rol —administrador, mecánico y
  los demás— llega al final del alcance, no en esta etapa.
- El `compose.yaml` es portable: imágenes OCI públicas, volúmenes nombrados y
  ninguna dependencia de funciones exclusivas de Docker Desktop (`AGENTS.md`
  §3.1; `ARCHITECTURE.md` RST-02).
- Los secretos reales van por variables de entorno o por un gestor de secretos:
  nunca se escriben en la imagen ni se versionan. Las credenciales del entorno
  local del taller son valores desechables y quedan acotadas a ese entorno.
- Cuando exista infraestructura definitiva, el despliegue a staging ocurrirá al
  integrar y producción exigirá una aprobación manual separada (líder técnico y
  responsable funcional). Hoy `AMB-02` y `AMB-03` en `ARCHITECTURE.md` siguen
  pendientes de infraestructura.

## Code Style

- **Frontend**: Angular 22 con componentes standalone, señales y formularios
  reactivos. **Prettier es la fuente de verdad** (`.prettierrc`: `printWidth` 100,
  comillas simples, parser `angular` para HTML) y `.editorconfig` fija 2 espacios,
  UTF-8 y comillas simples en TypeScript. El código se entrega formateado; nadie
  revisa estilo a mano.
- **Backend**: Java 25 sobre Spring Boot 4.1.2, Maven Wrapper por servicio,
  paquetes `com.subocol.<servicio>`, clases en PascalCase y Lombok para el
  boilerplate. La indentación sigue lo que genera Spring Initializr (tabuladores)
  y no reformateamos archivos generados para imponer otro estilo.
- **Linters**: por ahora solo **Prettier y `.editorconfig`**. No hay ESLint,
  Checkstyle ni Spotless, y no se agregan en esta etapa: entran cuando exista CI,
  y desde ese momento la configuración del repositorio manda y el incumplimiento
  bloquea la integración.
- **Prioridad de la etapa**: que el front se conecte al back respetando la
  arquitectura. El estilo se resuelve con el formateador; el esfuerzo se pone en
  la costura entre capas.
- **Buenas prácticas de desarrollo**: SOLID, ACID, separación por dominio,
  transacciones explícitas en las fronteras y ninguna regla de negocio en los
  controladores. Es criterio de revisión y de diseño, no una regla automatizada.
- **Nombres**: identificadores, rutas y código en inglés, sin abreviaturas;
  paquetes y módulos por dominio (`order`, `appointment`, `notification`), no por
  tipo técnico. Archivos de configuración en minúsculas (`application.yaml`); las
  migraciones Flyway seguirán `V<n>__<descripcion>.sql` en cada servicio cuando
  existan.
- **Idioma**: documentación, artefactos de arquitectura y comunicación con el
  equipo en español. Los logs de negocio que simulan WhatsApp son la excepción
  deliberada: son el entregable observable del worker y se leen como mensajes al
  cliente.
- **Auditoría y trazas**: cada servicio registra en logs estructurados con
  identificador de correlación; no se loguean datos personales del cliente ni del
  vehículo sin enmascarar.
## Forbidden

<!-- Team-specific forbidden patterns -->

## Mandated

<!-- Team-specific mandates -->

## Corrections

<!-- Self-learning loop appends here. -->
