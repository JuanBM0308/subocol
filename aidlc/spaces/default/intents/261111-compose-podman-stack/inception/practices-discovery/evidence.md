# Evidencia — Practices Discovery

> Qué inspeccioné, qué inferí de ello y con cuánta fuerza, qué decidió la persona
> en la entrevista y qué queda abierto. La fuerza de cada inferencia se marca:
> **Fuerte** (una persona lo dejó por escrito o la configuración lo determina),
> **Media** (una sola pieza de evidencia consistente), **Débil** (silencio o
> indicio único; la práctica se propone, no se afirma). Las respuestas de la
> entrevista convierten una inferencia en decisión: no suben su fuerza, cambian
> su origen de "evidencia" a "afirmación humana".

## Fuentes inspeccionadas

| Fuente | Qué mostró |
|---|---|
| `AGENTS.md` | Restricciones duras del taller: stack obligatorio, Database per Service, historial append-only con hash encadenado, cupos con bloqueo pesimista, cancelación silenciosa, reapertura con rol `SUPERVISOR` y motivo, Podman/Compose portable, publicación de solo 8080/8081/15672, registro obligatorio de propuestas rechazadas en `DECISIONES.md`, cobertura ≥80 % con JUnit + Mockito, monitoreo y auditoría en base de datos. **Es la fuente de verdad de las restricciones.** |
| `ARCHITECTURE.md` (509 líneas) | Arquitectura TO-BE: microservicios `Order`/`Appointment`/`Notification Worker`, gateway, Keycloak, PostgreSQL con `orders_db` y `appointments_db` en una instancia lógica, RabbitMQ con outbox, Flyway, OpenAPI, Nginx. Define ambientes AMB-01 (desarrollo local, implementable), AMB-02 (QA) y AMB-03 (producción) como destinos lógicos pendientes. Fija puertos de referencia 8080/8081/8180/15672/5432 y el comando `podman compose up --build`. Sus tablas TAC/PAT/RST/RF son la fuente de las reglas duras. |
| `CHALLENGE.md` (12 líneas) | **Enunciado del cliente y fuente de verdad de lo que se le debe.** Detalle en la sección siguiente. |
| `DECISIONES.md` | Documento del desarrollador: registra seis decisiones y las propuestas descartadas. Se leyó para entender el contexto, pero **no se usa como fuente de reglas ni de requisitos**: la autoridad es `AGENTS.md`, y lo que se le debe al cliente lo fija `CHALLENGE.md`. La única obligación que produce es la de seguir alimentándolo como entregable (`AGENTS.md` §4; `CHALLENGE.md`). |
| `compose.yaml` (raíz, 59 líneas) | Ya existe y está versionado en el commit inicial. Detalle en la sección siguiente: **no cumple** las restricciones del taller, y la persona confirmó que se reescribe desde cero. |
| `README.md` | Declara requisitos (Podman, Java 25/Spring Boot 4.x, Node/Angular CLI) y arranca una guía de ejecución local que **queda cortada en la línea 16**, sin cerrar el bloque de código. |
| Historial de git | Dos commits: `cdb2cc5 Initial commit subocol challenge` en `main`, y `9c35f9a Add architecture contex to agents` en la rama de trabajo. `main` tiene un solo commit; la rama de arquitectura está un commit adelante y **no está integrada**. |
| Ramas | `main`, `feature/realease/1-architecture` (local y en `origin`). No hay `develop`, `release/*` ni `hotfix/*`. El remoto es GitHub (`github.com/JuanBM0308/subocol`). |
| Árbol de trabajo | Limpio salvo el registro de AI-DLC no versionado y `CHALLENGE.md`. No hay cambios sin confirmar en código. |
| Build backend (`pom.xml` × 3) | Spring Boot 4.1.2, Java 25, Maven Wrapper. `order` y `appointment` traen `amqp`, `data-jpa`, `flyway`, `validation`, `webmvc`, driver PostgreSQL y Lombok; `notification-worker` solo `amqp` + `spring-rabbit-stream` y Lombok. Dependencias de prueba por starters `*-test` (JUnit 5 disponible por `org.junit.jupiter.api`). **No se declara Mockito**, no hay JaCoCo, Checkstyle ni Spotless. |
| Código backend | Tres aplicaciones Spring generadas por Initializr y una prueba cada una: `@SpringBootTest` con `contextLoads()`. Los `application.yaml` solo fijan `spring.application.name`. Indentación con tabuladores. |
| Frontend (`package.json`, `angular.json`, `.prettierrc`, `.editorconfig`) | Angular 22.1 + SSR (`@angular/ssr`, Express), test builder `@angular/build:unit-test` con Vitest 4 y jsdom, Prettier 3 como dependencia. Sin ESLint, sin script `format` ni `lint` en `package.json`. `.prettierrc`: `printWidth` 100, comillas simples, parser `angular` para HTML. `.editorconfig`: 2 espacios, UTF-8, comillas simples en TS, `trim_trailing_whitespace`. La app usa `signal()` y componentes standalone (patrón de Angular 22). |
| CI | **Ninguna configuración**: no hay `.github/workflows`, `.gitlab-ci.yml`, `Jenkinsfile`, `.circleci`, `azure-pipelines.yml` ni equivalentes. |

## `CHALLENGE.md` como fuente de verdad del cliente

El enunciado del cliente entra al registro como el documento que manda sobre lo
que se entrega. Lo que dice, y lo que implica para este taller:

- **Un taller automotriz** que hoy trabaja con Excel y WhatsApp y necesita un
  sistema donde el asesor registre la orden de servicio con cliente, vehículo,
  aseguradora si aplica y el trabajo a realizar. Es el dominio del sistema, no un
  caso genérico.
- **La orden avanza por estados** hasta repararse y cerrarse, y a veces se reabre
  siempre con una razón. Implica una máquina de estados explícita y un motivo
  obligatorio, no un campo libre de texto.
- **El cliente tiene citas** para traer o recoger el vehículo, y **cada asesor
  atiende un número limitado de citas por franja horaria**. Implica cupos por
  asesor y por franja, con control de concurrencia: es la razón de ser del
  `Appointment Service` y del `AdvisorSlot`.
- **El cliente recibe un WhatsApp** cuando se le agenda o cancela una cita, y
  **el WhatsApp se puede simular con un log**. El canal observable es `stdout`
  estructurado; no hay integración real con WhatsApp en el alcance.
- **Al cerrar una orden, sus citas pendientes se cancelan solas y en ese caso no
  se le avisa al cliente**, porque ya recogió su vehículo. Es el requisito que
  obliga al flag `notifyCustomer` y a la supresión del log: el silencio es
  comportamiento exigido, no una omisión.
- **Auditoría pide saber quién cambió cada estado y cuándo, sin que nadie pueda
  alterar ese registro.** Implica historial append-only con `changedBy` del JWT y
  protección contra `UPDATE`/`DELETE`; no es un log de aplicación opcional.
- **Las órdenes de aseguradoras grandes llegarán más adelante por integración, no
  las crea el asesor.** Es alcance futuro explícito: se documenta la frontera y
  se rechaza su creación manual, sin implementar la integración.
- **Restricciones técnicas**: frontend Angular, backend Java con microservicios y
  todo levantando con un solo comando de compose. Fija el stack y la topología.
- **Tiempo: 1 día**, con uso libre de IA. Implica que el alcance se prioriza y
  justifica: lo que queda fuera se documenta, no se implementa a medias.
- **Entregables**: el repositorio, **un diagrama de contexto** con los dominios y
  cómo se hablan, y un **`DECISIONES.md`** con cada frontera trazada, las
  alternativas descartadas y **al menos 2 propuestas de la IA rechazadas**. Los
  tres son obligatorios y se revisan al cierre.

La persona nombra `CHALLENGE.md` como la fuente de verdad de lo que se le debe al
cliente (pregunta 8). El estándar del taller no lo contradice: `AGENTS.md` §3
sustituye Docker por Podman por incompatibilidad del entorno, y `ARCHITECTURE.md`
(RST-02) documenta esa sustitución.

## Decisiones de la entrevista

Ocho preguntas cerradas con la persona; la síntesis consolidada fue confirmada
("Looks correct"). Cada respuesta se lee así:

1. **Q1 — Integración de ramas.** Todavía no hay un `main` estable: la única rama
   publicada es `feature/realease/1-architecture`, los commits van en inglés y en
   orden normal. *Resuelve* el idioma de los mensajes y descarta el squash como
   práctica. *Deja abierto* la política de integración (squash frente a merge
   commit), que se fija antes de la primera integración real.
2. **Q2 — Rebanada vertical.** Sí, siempre: se verifican partes pequeñas de punta
   a punta a medida que se implementan, para no llevarse sustos cuando se armen
   los bloques completos. *Resuelve* la postura y la vuelve afirmación humana;
   *no deja nada abierto*.
3. **Q3 — Cobertura.** 80 % de líneas, JaCoCo en backend y umbral de Vitest en
   frontend, y el build falla por debajo; la fuente de verdad es `AGENTS.md` §5 y
   `DECISIONES.md` es del desarrollador, no fuente para el agente. *Resuelve* la
   unidad de medida, las herramientas y la autoridad documental. *Deja abierto*
   la configuración concreta: ni JaCoCo ni el umbral existen todavía.
4. **Q4 — Cadencia de pruebas.** Después de implementar cada capa verificable, con
   la suite completa en verde antes de integrar; hoy solo existen los esqueletos
   de front y back. *Resuelve* la cadencia como test-after y confirma el estado
   real del código. *Deja abierto* nada de la cadencia.
5. **Q5 — Entornos.** Todo local con Podman (`podman compose up --build`) y
   verificación antes de cerrar la entrega; staging y producción quedan como
   destino. *Resuelve* la topología de entornos y la cadencia de despliegue.
   *Deja abierto* el pipeline de CI, que no se pide ahora.
6. **Q6 — Linters.** Solo Prettier y `.editorconfig` por ahora; lo prioritario es
   que el front se conecte al back respetando la arquitectura, más buenas
   prácticas de desarrollo (SOLID, ACID). *Resuelve* que no se agregan linters en
   esta etapa y fija la prioridad del trabajo. *Deja abierto* cuándo entran
   ESLint/Checkstyle/Spotless.
7. **Q7 — Base de datos.** Un usuario y una contraseña por base, con permisos
   solo sobre su propia base, según `ARCHITECTURE.md` (TAC-005); el acceso por rol
   (admin, mecánico, etc.) llega al final del alcance. *Resuelve* el modelo de
   credenciales del `compose.yaml` y descarta el `postgres` compartido. *Deja
   abierto* el diseño de RBAC, que es de una etapa posterior.
8. **Q8 — `compose.yaml`.** Se reescribe desde cero sobre el estándar y la
   arquitectura; `CHALLENGE.md` manda sobre lo que se le debe al cliente.
   *Resuelve* que el archivo vigente se reemplaza, no se corrige por partes, y
   fija la autoridad del enunciado. *Deja abierto* el contenido final del archivo,
   que es trabajo de `infrastructure-design` y `environment-provisioning`.

## Corrección de citas del borrador

El borrador citaba `AGENTS.md` §2 con los identificadores que ese documento trae
entre paréntesis (TAC-003/004/005, PAT-005/007). Al contrastarlos con
`ARCHITECTURE.md`, la numeración **no coincide** con la de sus tablas: el
`SELECT FOR UPDATE` de cupos es `TAC-003`, el outbox es `TAC-004`, los usuarios de
base separados y los secretos son `TAC-005`, Database per Service es `PAT-003` y
el historial append-only es `PAT-004`. `PAT-005` es la máquina de estados y
`PAT-007` no existe. Las reglas de `discovered-rules.md` ahora citan `AGENTS.md`
por sección y `ARCHITECTURE.md` por el identificador verificable en su propia
tabla; nadie hereda la numeración equivocada. También se retiró `RST-08` como
fuente de la regla de secretos: ese riesgo habla de estados y cupos sin definir,
no de credenciales.

## Estado del `compose.yaml` existente

Se leyó antes de inferir nada sobre topología de contenedores. Lo que hay hoy:

1. Declara `version: '3.8'`, clave obsoleta para la especificación Compose actual (se ignora con advertencia).
2. Un solo servicio `postgres` (`postgres:16-alpine`) que intenta crear dos bases con `POSTGRES_MULTIPLE_DATABASES`. La imagen oficial de PostgreSQL **no interpreta esa variable**: solo crea `POSTGRES_DB` (ausente aquí), así que `orders_db` y `appointments_db` no llegan a existir. Tampoco hay script de entrypoint propio que las cree.
3. Monta `./init-db.sql` en `/docker-entrypoint-initdb.d/init-db.sql`, pero **ese archivo no existe en el repositorio** (ni versionado ni en disco). En el mejor caso el montaje queda vacío; en algunos runtimes se crea un directorio en su lugar.
4. No define `networks:` ni `workshop-net`; todo queda en la red por defecto.
5. Publica `5432:5432` de PostgreSQL al host, contra la restricción de dejar las bases en contenedores aislados y publicar solo 8080/8081/15672.
6. No hay servicio de gateway (8081) ni de identidad (8180), aunque `ARCHITECTURE.md` los sitúa en la topología.
7. Los `build:` apuntan a `./subocol-order-service`, `./subocol-appointment-service`, `./subocol-notification-worker` y `./subocol-frontend`, pero **no existe ningún `Dockerfile` en el repositorio**, y en los tres servicios Java el `pom.xml` real vive un nivel más abajo (`subocol-order-service/subocol-order-service/pom.xml`), por lo que el contexto de build tampoco contiene el proyecto Maven.
8. `depends_on` sin `healthcheck`: los servicios pueden arrancar antes de que PostgreSQL acepte conexiones.
9. Credenciales únicas (`postgres`/`postgrespassword`) compartidas por ambas bases, sin usuarios por servicio; RabbitMQ sin credenciales explícitas (queda `guest`/`guest`).
10. Sin políticas de reinicio, sin healthchecks y sin límites de recursos.

Nada de esto invalida las prácticas afirmadas —el archivo es trabajo pendiente de
este mismo intent y la persona decidió reescribirlo—, pero la etapa de requisitos
hereda el diagnóstico: **el archivo existe, está versionado y no cumple el
estándar**.

## Inferencias por sección

### Way of Working — **Media**, con un pendiente explícito

Hay `main` como única rama permanente, una rama de trabajo con nombre por tema
(`feature/realease/1-architecture`) publicada al remoto, y ningún rastro de
`develop`/`release`. `ARCHITECTURE.md` (AMB-01) dice explícitamente «crear ramas
por historia y ejecutar pruebas unitarias». Eso describe un flujo de ramas cortas
al estilo GitHub Flow. La fuerza es media porque hay un solo caso: no se observa
ningún pull request integrado ni la política de merge. La entrevista confirmó el
idioma de los commits y que **no hay `main` estable todavía**, y dejó la política
de integración como pendiente: que un borrador no pueda afirmarla es exactamente
el resultado correcto. El nombre de la rama actual trae un error de tipeo
(`realease`), señal de que la convención no está verificada por automatización.

### Walking Skeleton — **Fuerte por afirmación humana**

La evidencia sola era silencio (**Débil**): el repositorio ya viene andamiado con
los cuatro componentes, lo que sugería que la rebanada vertical se consideraba
hecha. La persona corrigió esa lectura: **sí, siempre**, se verifican partes
pequeñas de punta a punta a medida que se implementan. El borrador proponía no
correr la ceremonia; la respuesta lo reemplaza y ahora la práctica vive en
`team-practices.md` con verificación ejecutable (`podman compose up --build` más
una prueba que cruce la frontera recién conectada). El primer corte pendiente es
front → gateway → `order-service` → `orders_db`, porque los esqueletos existen
pero no están conectados.

### Testing Posture — **Fuerte** en el piso, **Fuerte** en la cadencia tras Q4, **Débil** en el tooling

- El piso de 80 % está escrito por una persona en `AGENTS.md` §5: **fuerte**, y
  por eso vive también en `discovered-rules.md` como restricción dura.
- La cadencia es test-after y la persona la confirmó: se implementa cada capa
  verificable y después se escriben y ejecutan sus pruebas, con la suite completa
  en verde antes de integrar. Lo único que existe hoy son pruebas de contexto
  generadas con el andamiaje y un spec de Angular que verifica el título
  renderizado; no hay historial de commits con prueba previa ni mención de
  TDD/BDD/ATDD.
- El tooling sigue siendo el hueco: no hay JaCoCo ni umbral en los `pom.xml`, no
  se declara Mockito de forma explícita, `package.json` no tiene script de
  cobertura y `angular.json` no fija umbrales para Vitest. La entrevista decidió
  **qué** se mide (80 % de líneas, JaCoCo y Vitest, build que falla por debajo);
  **falta implementarlo**, y eso queda anotado como práctica a construir, no como
  práctica vigente.
- Las pruebas de concurrencia, idempotencia y append-only que el taller exige
  (`AGENTS.md` §2; `ARCHITECTURE.md` TAC-003, TAC-004, PAT-004) aún no existen; se
  registran igual: como práctica a construir.

### Deployment — **Fuerte**

Todo apunta a un único entorno real: local con Podman Compose. `AGENTS.md` §3 fija
el comando primario, `ARCHITECTURE.md` fija la red privada, los puertos y la
portabilidad OCI, y AMB-02/AMB-03 quedan explícitamente «pendientes de
infraestructura definitiva». No hay pipeline, ni registry, ni configuración de
staging o producción, ni etiquetas de versión en git. La disciplina de «levantar y
verificar antes de cerrar una entrega» era inferencia mía y la persona la
confirmó en Q5. Q7 agregó el modelo de credenciales (usuario por base, permisos
sobre su propia base), y Q8 que el `compose.yaml` se reescribe desde cero.

### Code Style — **Fuerte** en frontend, **Media** en backend, **Débil** en lo demás

- Prettier está configurado y es dependencia declarada; `.editorconfig` fija
  espacios, comillas y saltos de línea: **fuerte** para el frontend.
- Los tres servicios usan tabuladores y las convenciones de Initializr; Lombok
  está declarado: **media** (lo determina la herramienta, no una decisión
  escrita).
- La ausencia de ESLint, Checkstyle y Spotless es un hecho, y Q6 confirmó que
  **por ahora se sostiene solo con Prettier**: ya no es una incógnita. Los
  linters entran cuando haya CI. La persona agregó la prioridad que no estaba en
  ningún documento: conectar front con back respetando la arquitectura, con
  buenas prácticas de desarrollo (SOLID, ACID) como criterio.
- «Paquetes por dominio, código en inglés, documentación en español» es lectura de
  lo que existe (paquetes `com.subocol.subocol_*`, documentos en español, commits
  en inglés): **media** ahora, porque Q1 confirmó que los commits van en inglés.
- `README.md` cortado en la línea 16 y `ARCHITECTURE.md` con tablas desalineadas
  indican que la documentación se escribe a mano sin verificación de formato: no
  se convierte en regla, se anota como deuda.

## Evidencia ausente y límites de esta etapa

- **Sin artefactos de reverse-engineering.** `code-structure`, `technology-stack`,
  `dependencies`, `code-quality-assessment`, `architecture` y `business-overview`
  no existen en el registro: reverse-engineering está en SKIP dentro de este plan.
  Son entradas `conditional_on: brownfield` y su ausencia no es un hueco de
  cobertura; la inspección directa del workspace las sustituye, con la salvedad de
  que no hay análisis de estructura ni evaluación de calidad de código.
- **Sin CI que observar.** No hay evidencia de gates, tiempos de pipeline ni
  caching; la etapa `ci-pipeline` (3.7) los definirá desde cero. La persona no
  pidió pipeline en esta etapa (Q5), así que no se afirma ninguno.
- **Sin historial suficiente.** Dos commits no permiten inferir tamaño de PR,
  frecuencia de integración ni vida real de las ramas. Por eso la política de
  integración queda abierta.
- **Sin ejecución.** No levanté el stack ni corrí las suites: `compose.yaml`
  referencia un `init-db.sql` inexistente y no hay `Dockerfile`, así que un
  `podman compose up --build` fallaría en el build antes de probar nada. Tampoco
  ejecuté Maven ni npm (fuera del alcance de esta etapa y no pedido por el plan).

## Incertidumbres que quedan abiertas

Ninguna bloquea la afirmación de prácticas; todas tienen dueño en una etapa
posterior:

1. **Política de integración**: squash frente a merge commit sigue sin decidirse.
   Dueño: el equipo, antes de la primera integración real (Q1).
2. **Instrumentación de cobertura**: JaCoCo no está en los `pom.xml`, Mockito no
   está declarado, el umbral de Vitest no está fijado y no hay script de
   cobertura. Dueño: `ci-pipeline` (3.7) y el primer corte de código.
3. **Entrada de linters**: cuándo se agregan ESLint (frontend) y
   Checkstyle/Spotless (backend). Depende de que exista CI; hoy no se agregan (Q6).
4. **Puerto de identidad**: `ARCHITECTURE.md` sitúa Keycloak en 8180, mientras
   `AGENTS.md` §3.2 enumera solo 8080, 8081 y 15672 como puertos publicados.
   Dueño: `infrastructure-design`, al definir la topología publicada.
5. **Contenido del `compose.yaml`**: se reescribe desde cero, pero el archivo
   final —usuarios por base, `init-db.sql`, healthchecks, `Dockerfile` por
   componente— es trabajo de `infrastructure-design` y
   `environment-provisioning` (Q7, Q8).
6. **RBAC por rol** (administrador, mecánico y los demás): llega al final del
   alcance, no en esta etapa (Q7).

## Contexto que heredan las etapas siguientes

- El `compose.yaml` versionado no cumple el estándar y su montaje
  `docker-entrypoint-initdb.d` apunta a un archivo que no existe: la etapa de
  requisitos parte de un archivo que hay que **reemplazar**, no de una hoja en
  blanco, y así lo decidió la persona (Q8).
- No hay `Dockerfile` en ninguno de los cuatro componentes y los contextos de
  build del compose apuntan a directorios que no contienen el `pom.xml`: cualquier
  verificación real del stack exige resolver eso primero (probablemente en
  `infrastructure-design` y `environment-provisioning`).
- El piso de 80 % de líneas con JaCoCo y Vitest es una restricción humana, no una
  preferencia: queda registrada en `discovered-rules.md` para que `ci-pipeline` la
  haga cumplir, y en `team-practices.md` como práctica a implementar.
- Los tres entregables del cliente —repositorio, diagrama de contexto y
  `DECISIONES.md` con al menos dos propuestas de la IA rechazadas— están en
  `CHALLENGE.md` y condicionan el cierre; `DECISIONES.md` se alimenta como
  entregable, nunca como fuente de reglas.
- La rebanada vertical es la unidad de avance: el primer corte pendiente es
  front → gateway → `order-service` → `orders_db`, y el siguiente el camino de
  eventos con RabbitMQ y el worker.
