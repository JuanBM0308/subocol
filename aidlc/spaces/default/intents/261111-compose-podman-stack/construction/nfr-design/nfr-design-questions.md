# Preguntas — NFR Design

> Esta etapa convierte los objetivos no funcionales ya acordados en soluciones de
> diseño concretas: dónde vive cada control, con qué mecanismo y con qué coste.
> Los requisitos de `nfr-requirements/` no se re-discuten; estas preguntas eligen
> cómo se implementan.

## Q1. [seguridad] ¿Cómo llega el navegador al gateway?

El frontend se sirve en 8080 y el gateway escucha en 8081: son orígenes distintos, así que hay que decidir quién resuelve esa costura.

- A. CORS configurado en el gateway con el origen `http://localhost:8080` permitido, y el frontend llama a `http://localhost:8081/api/orders`
- B. nginx hace de proxy inverso: `/api/*` se reenvía al gateway, el frontend usa la ruta relativa `/api/orders` y no hay CORS porque todo es el mismo origen
- C. Las dos: proxy en nginx y CORS en el gateway para las llamadas directas
- X. Other (please specify)

[Answer]: B. nginx hace de proxy inverso: `/api/*` se reenvía al gateway, el frontend usa la ruta relativa `/api/orders` y no hace falta CORS porque todo es el mismo origen. — (**Mode:** chat, 2026-11-11T14:45:27Z)

## Q2. [observabilidad] ¿Qué expone Actuator y en qué puerto?

El `healthcheck` de los contenedores Java necesita una forma barata de preguntar por la salud real.

- A. Solo `/actuator/health` (con detalle de dependencias) y `/actuator/info`, en el mismo puerto del servicio
- B. Igual que A, pero con un puerto de management propio (8090) que no se publica y al que solo llega el `healthcheck` dentro de la red
- C. `/actuator/health` en el puerto normal y el resto de Actuator solo en el puerto de management
- X. Other (please specify)

[Answer]: A. Solo `/actuator/health` (con detalle de dependencias) y `/actuator/info`, en el mismo puerto del servicio. — (**Mode:** chat, 2026-11-11T14:45:27Z)

## Q3. [observabilidad] ¿Cómo se producen los logs JSON?

Spring Boot 4 trae logging estructurado propio, pero la alternativa clásica sigue disponible.

- A. Logging estructurado nativo de Spring Boot 4, sin dependencia extra, configurado desde `application.yaml`
- B. `logstash-logback-encoder` con un `logback-spring.xml` propio, que da control total del formato
- C. Un encoder propio de Logback, escrito a medida para el proyecto
- X. Other (please specify)

[Answer]: A. Logging estructurado nativo de Spring Boot 4, sin dependencia extra, configurado desde `application.yaml`. — (**Mode:** chat, 2026-11-11T14:45:27Z)

## Q4. [observabilidad] ¿Cómo se propaga el `correlationId`?

El requisito pide que el `correlationId` viaje del gateway al servicio y aparezca en cada línea de log.

- A. Un filtro propio en el gateway que genera o propaga la cabecera `X-Correlation-Id` y lo deja en el MDC de cada servicio
- B. Micrometer Tracing, que ya resuelve la correlación, aunque añade instrumentación y una dependencia
- C. Solo en el gateway: el identificador viaja en la cabecera, pero los servicios no lo exponen en sus logs
- X. Other (please specify)

[Answer]: A. Un filtro propio en el gateway que genera o propaga `X-Correlation-Id` y lo deja en el MDC de cada servicio. — (**Mode:** chat, 2026-11-11T14:45:27Z)

## Q5. [rendimiento] ¿Qué valores tiene el pool de conexiones?

PostgreSQL y los servicios comparten el host del taller, así que conviene acotar el consumo.

- A. HikariCP con valores explícitos en ambos servicios: mínimo 2 conexiones, máximo 10, timeout de conexión 5 s y máximo de vida 30 min
- B. Los valores por defecto de Spring Boot, sin tocar la configuración
- X. Other (please specify)

[Answer]: B. Los valores por defecto de Spring Boot, sin tocar la configuración del pool. — (**Mode:** chat, 2026-11-11T14:45:27Z)

## Q6. [fiabilidad] ¿Cuándo se aplican las migraciones Flyway?

`requirements.md` da la propiedad del esquema a Flyway; falta decidir dónde se ejecuta.

- A. Al arrancar cada servicio, con `ddl-auto=validate` en JPA para que la aplicación nunca cree el esquema por su cuenta
- B. En un job de migración aparte que corre antes que los servicios, y los servicios arrancan con `ddl-auto=none`
- C. Al arrancar cada servicio, con `ddl-auto=none` y sin validación posterior
- X. Other (please specify)

[Answer]: A. Al arrancar cada servicio, con `ddl-auto=validate` en JPA para que la aplicación nunca cree el esquema por su cuenta. — (**Mode:** chat, 2026-11-11T14:45:27Z)

## Q7. [seguridad] ¿Quién es el dueño de las tablas de cada base?

El historial debe ser inmutable incluso frente a la propia aplicación. La respuesta decide si hace falta un rol extra.

- A. Un solo rol por base: es dueño de sus tablas y el trigger de PostgreSQL bloquea `UPDATE` y `DELETE` sobre el historial
- B. Dos roles por base: un rol propietario que solo usa Flyway para migrar y un rol de aplicación con permisos DML, sin `UPDATE` ni `DELETE` sobre el historial
- C. Un solo rol por base, sin trigger: la inmutabilidad depende del código de la aplicación
- X. Other (please specify)

[Answer]: B. Dos roles por base: un rol propietario que solo usa Flyway para migrar y un rol de aplicación con permisos DML, sin `UPDATE` ni `DELETE` sobre el historial. — (**Mode:** chat, 2026-11-11T14:45:27Z)

## Q8. [fiabilidad] ¿Cómo se comprueba la salud de los servicios Java en el contenedor?

El `healthcheck` necesita una herramienta que exista en la imagen de runtime.

- A. Instalar `curl` en la imagen de runtime y consultar `/actuator/health`
- B. Usar el `wget` que ya viene en la imagen base, sin instalar nada
- X. Other (please specify)

[Answer]: A. Instalar `curl` en la imagen de runtime y consultar `/actuator/health`. — (**Mode:** chat, 2026-11-11T14:45:27Z)

## Consolidated Summary Confirmation

- **Q1 — Camino del navegador al gateway:** nginx hace de proxy inverso: `/api/*` se reenvía al gateway, el frontend usa la ruta relativa `/api/orders` y no hay CORS porque el navegador habla con el mismo origen. El gateway conserva un filtro CORS restringido al origen del frontend como control en profundidad, pero el flujo no depende de él.
- **Q2 — Actuator:** solo `/actuator/health` (con detalle de dependencias) y `/actuator/info`, expuestos en el mismo puerto de cada servicio; sin puerto de management aparte.
- **Q3 — Logs JSON:** logging estructurado nativo de Spring Boot 4, sin dependencia extra, configurado desde `application.yaml`.
- **Q4 — `correlationId`:** filtro propio en el gateway que genera o propaga `X-Correlation-Id`, y en cada servicio un filtro que lo deja en el MDC para que salga en cada línea de log.
- **Q5 — Pool de conexiones:** los valores por defecto de Spring Boot, sin tocar la configuración del pool.
- **Q6 — Migraciones Flyway:** se aplican al arrancar cada servicio, con `ddl-auto=validate` en JPA para que la aplicación nunca cree el esquema por su cuenta.
- **Q7 — Dueño de las tablas:** dos roles por base — un rol propietario que solo usa Flyway para migrar y un rol de aplicación con permisos DML, sin `UPDATE` ni `DELETE` sobre el historial. **Consecuencia a tener en cuenta:** los scripts de inicialización crearán cuatro roles en total (dos propietarios y dos de aplicación), todos confinados a su propia base; la regla de la persona —un rol de aplicación por base y ningún acceso cruzado— se mantiene intacta, pero la redacción de FR3.1 («los dos roles») queda ampliada por esta decisión de diseño.
- **Q8 — Healthcheck de los servicios Java:** se instala `curl` en la imagen de runtime y el `healthcheck` consulta `/actuator/health`.

Does this all look correct before I generate the design artifacts?

- Looks correct
- Request changes

[Answer]: Looks correct
