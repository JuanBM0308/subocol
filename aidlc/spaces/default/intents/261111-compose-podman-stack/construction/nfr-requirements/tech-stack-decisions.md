# Decisiones de stack tecnológico — Stack Podman y rebanada de órdenes

> Etapa NFR Requirements. Deriva de `requirements.md` (NFR3, NFR6, NFR9) y de las
> respuestas confirmadas en `nfr-requirements-questions.md` (Q3, Q6, Q7). Contexto
> de referencia: `requirements-analysis/requirements.md`, `AGENTS.md` §1 y §5,
> `ARCHITECTURE.md` (FRM-003, FRM-011, RST-02), `team-practices.md`.
> El stack obligatorio está fijado por `AGENTS.md` y `ARCHITECTURE.md`; aquí se
> registra qué se usa exactamente, con qué alternativa se descartó y con qué
> criterio.

## Requisitos derivados

| ID | Requisito | Criterio de verificación |
|----|-----------|--------------------------|
| **NFR6.1** | Podman con Podman Compose es el orquestador del proyecto; `podman-compose` queda como alternativa documentada y comprobada. | `podman compose up --build` levanta el stack y, además, `podman-compose config` valida el mismo `compose.yaml` con la implementación alternativa; el resultado de ambas comprobaciones queda escrito en la documentación de despliegue. Si el provider de compose no está disponible, `podman-compose up --build` es la vía de arranque y queda registrada también su ejecución. |
| **NFR6.2** | Todo componente corre en imágenes OCI públicas y portables; ninguna dependencia de funciones exclusivas de Docker Desktop. | El compose no usa `depends_on` con condiciones exóticas, volúmenes de tipo no soportado niprivileged; el stack levanta con `podman compose` sin `docker`. |
| **NFR6.3** | El stack de aplicación es Java 25 con Spring Boot 4.1.2 en los servicios, Spring Cloud Gateway como entrada, Angular 22 en el frontend, PostgreSQL 16 y RabbitMQ como broker. | Las versiones de los `pom.xml`, del `package.json` y de las imágenes del compose coinciden con lo declarado. |
| **NFR6.4** | El frontend se sirve como build de producción estático detrás de nginx, con el SSR desactivado. | La imagen del frontend no ejecuta `ng serve`; sirve archivos estáticos y la imagen no incluye Node en la etapa final. |
| **NFR3.1** | El backend mide cobertura de líneas con JaCoCo y el build falla por debajo del 80 % por componente. | `./mvnw verify` falla cuando la cobertura baja del 80 %. |
| **NFR3.2** | El frontend mide cobertura con el umbral de Vitest y el build falla por debajo del 80 %. | `npm test` falla cuando la cobertura baja del 80 %. |
| **NFR3.3** | Las pruebas se escriben después de cada capa verificable (JUnit 5 y Mockito en el backend, Vitest en el frontend) y la suite completa queda en verde antes de integrar. | La suite del componente afectado y la suite completa del componente pasan antes de integrar. |
| **NFR9.1** | El flujo registrar → listar → detalle se completa en cinco pasos o menos, con formularios reactivos y validación en cliente y servidor. | Recorrido del flujo en el navegador con conteo de pasos y mensajes de error visibles. |

## Decisiones y alternativas descartadas

### D1 — Un contenedor PostgreSQL con dos bases en lugar de dos contenedores

**Contexto.** `AGENTS.md` exige una base por servicio y prohíbe compartir una base
relacional entre servicios. `ARCHITECTURE.md` (modelo de datos) dice que
`orders_db` y `appointments_db` pueden residir en la misma instancia PostgreSQL
durante el desarrollo. La persona eligió explícitamente la opción liviana.

**Decisión.** Un solo contenedor PostgreSQL 16 con dos bases independientes y dos
roles con permisos solo sobre su base.

**Consecuencias.** Positivas: una imagen, un volumen, un `healthcheck`, menos
procesos que levantar en un taller de un día. Negativas: los dos servicios comparten
el ciclo de vida y los recursos del contenedor; un problema de memoria o de disco
afecta a los dos. Neutras: la frontera de datos se mantiene porque ninguna base se
comparte en sus tablas ni en sus permisos.

**Alternativas descartadas.** *Dos contenedores PostgreSQL*: aíslan mejor los
recursos, pero duplican definición, healthcheck y mantenimiento en un entorno que
no tiene el problema que resolvería. *Una base con un esquema por servicio*:
compartiría una base física, que es exactamente lo que la regla del proyecto
prohíbe.

### D2 — nginx sirviendo el build estático, sin SSR

**Contexto.** El proyecto Angular trae andamiaje de SSR. El flujo exigido es un
formulario y un listado en el navegador.

**Decisión.** Build de producción estático servido por nginx; el SSR se desactiva.

**Consecuencias.** Positivas: imagen final sin runtime de Node, menos superficie y
despliegue más simple. Negativas: no hay renderizado en servidor; si más adelante
hace falta SEO o prerenderizado, hay que reintroducir Node. Neutras: la API se
consume igual desde el navegador.

**Alternativas descartadas.** *Angular SSR sobre Node*: aporta complexities sin
necesidad para una herramienta interna de taller. *`ng serve` dentro del
contenedor*: es un servidor de desarrollo; no es aceptable en una imagen de
entrega.

### D3 — Sin SLO local, sin métricas ni trazas

**Contexto.** El ambiente es local, de un día, con un usuario previsto. La persona
priorizó acelerar hacia la lógica de negocio.

**Decisión.** Sin SLO formal; el criterio de fiabilidad es el arranque saludable y
la recuperación automática. Observabilidad mínima: logs JSON con `correlationId`,
Actuator health y management del broker.

**Consecuencias.** Positivas: nada de infraestructura de medición que alguien
mire. Negativas: no hay datos de uso para dimensionar ni para detectar degradación
temprana. Neutras: Actuator deja las métricas de Micrometer disponibles si más
adelante se activan.

**Alternativas descartadas.** *SLO local del 99 %*: sin una ventana de medición ni
historial, el número no se sostiene. *Prometheus y OpenTelemetry*: presupuesto de
operación y de implementación que el taller no tiene.

### D4 — SHA-256 con serialización canónica para el historial

**Contexto.** `order_status_history` es append-only y su detección de alteraciones
depende de una cadena de hashes (`AGENTS.md` §2.B.2, PAT-004). La revisión de
requisitos (R-01) pidió fijar el algoritmo y el formato exacto.

**Decisión.** SHA-256 en hexadecimal minúsculas sobre una serialización canónica con
prefijo de longitud: para cada uno de los ocho campos, en el orden fijo `eventId`,
`orderId`, `fromStatus`, `toStatus`, `reason`, `changedBy`, `changedAt`,
`previousHash`, se escribe `<longitudEnBytes>:<valor>` en UTF-8 y los ocho fragmentos
se unen con `|`. Un valor nulo se escribe `~` y un valor vacío `0:`. Los UUID van en
forma textual RFC 4122 en minúsculas y `changedAt` en ISO-8601 UTC con milisegundos.
La primera fila usa `previousHash` de 64 ceros.

**Consecuencias.** Positivas: el formato es verificable por cualquier persona y
reproducible; el génesis de ceros hace la primera fila verificable sin estado
previo. Negativas: cualquier cambio futuro en el formato obliga a recomputar el
historial, por lo que el formato se documenta y no se toca. Neutras: SHA-256 es
suficiente para detección; SHA-512 no aporta valor en este tamaño de fila.

**Alternativas descartadas.** *Hash sobre el JSON completo de la fila*: ataría la
huella al nombre de las columnas y al formato de serialización de la biblioteca,
haciendo el historial dependiente de detalles técnicos. *Separador `|` sin
escapado*: un `reason` de texto libre con ese carácter produciría digest distintos
en implementaciones conformes. *`previousHash` nulo en la primera fila*: obliga a
tratar el caso nulo como excepción permanente.

## Notas de estilo y prácticas aplicadas

- Identificadores, rutas y código en inglés; documentación y comunicación en
  español. Paquetes por dominio (`order`, `appointment`, `notification`).
- Prettier y `.editorconfig` como fuente de verdad del frontend; la indentación que
  genera Spring Initializr en el backend.
- Los secretos viajan por variables de entorno; nunca se escriben en la imagen ni se
  versionan.

## Fuentes

- `requirements-analysis/requirements.md` — NFR3, NFR6, NFR9 y restricciones del
  stack.
- `nfr-requirements-questions.md` — respuestas Q3, Q6 y Q7.
- `AGENTS.md` §1 (stack obligatorio) y §5 (cobertura).
- `ARCHITECTURE.md` — FRM-003 (Spring Cloud Gateway), FRM-011 (Keycloak), RST-02
  (portabilidad).
- `team-practices.md` — `## Testing Posture` y `## Code Style`.
