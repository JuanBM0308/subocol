# Preguntas — NFR Requirements

> Esta etapa fija los objetivos no funcionales del stack y de la primera rebanada
> de órdenes. Todo lo que ya está decidido en `requirements.md`, `AGENTS.md` y
> `ARCHITECTURE.md` se toma como base; estas preguntas cubren lo que falta para
> que cada objetivo sea medible.

## Q1. [rendimiento] ¿Qué latencia y carga se comprometen?

`requirements.md` pide menos de 1 segundo con hasta 1.000 órdenes, pero no fija percentil ni concurrencia, así que el objetivo no se puede verificar igual dos veces.

- A. p95 ≤ 500 ms para registrar y ≤ 800 ms para listar 1.000 órdenes, con 10 usuarios concurrentes
- B. p95 ≤ 1 s para registrar y para listar 1.000 órdenes, con 10 usuarios concurrentes
- C. Solo una prueba de humo con una orden y sin concurrencia
- D. p95 ≤ 300 ms asumiendo menos de 100 órdenes
- X. Other (please specify)

[Answer]: MVP: no se compromete percentil ni latencia de producción; se verifica con una prueba de humo (registrar y listar con una orden y un usuario, por debajo de 2 s) como línea base. Lo prioritario es demostrar el registro y el acceso con usuarios de prueba, que llegan con el bloque de autenticación. — (**Mode:** chat, 2026-11-11T13:59:31Z)

## Q2. [disponibilidad] ¿Se declara un SLO formal o basta el arranque local saludable?

El taller es local y de un día; un SLO de producción no tiene todavía dónde medirse.

- A. Sin SLO formal: el criterio es arrancar saludable y recuperarse con `restart: unless-stopped`; el SLO llega con staging
- B. SLO local del 99 % durante el horario del taller, medido con los healthchecks
- C. SLO de 99,9 % desde ya, como si fuera producción
- X. Other (please specify)

[Answer]: A. Arranque local saludable, sin SLO formal; la recuperación depende de `restart: unless-stopped` y el SLO llega con staging. — (**Mode:** chat, 2026-11-11T13:59:31Z)

## Q3. [seguridad] ¿Qué controles de seguridad entran en esta rebanada?

No hay autenticación todavía, pero el gateway y los formularios exponen superficie de entrada.

- A. Validación de entrada en gateway y servicios, CORS restringido al origen del frontend, secretos por variables y ningún puerto de datos publicado; la autenticación se difiere
- B. Lo mismo que A, más rate limiting en el gateway
- C. Solo secretos por variables y puertos sin publicar; validación y CORS quedan para después
- X. Other (please specify)

[Answer]: A acotado a lo indispensable: validación básica de entrada, CORS restringido al origen del frontend, secretos por variables y ningún puerto de datos publicado; sin rate limiting. Lo que se busca es desplegar rápido para definir back y front. — (**Mode:** chat, 2026-11-11T13:59:31Z)

## Q4. [observabilidad] ¿Cuál es el mínimo observable que se exige?

Hace falta decidir hasta dónde se invierte en observabilidad en esta rebanada.

- A. Logs JSON estructurados con `correlationId`, endpoints de salud de Actuator y management de RabbitMQ; sin métricas ni trazas
- B. Lo mismo que A, más métricas Micrometer/Prometheus expuestas por el gateway
- C. Lo mismo que A, más trazas distribuidas con OpenTelemetry
- X. Other (please specify)

[Answer]: A. Logs JSON estructurados con `correlationId`, endpoints de salud de Actuator y management de RabbitMQ; sin métricas ni trazas. — (**Mode:** chat, 2026-11-11T13:59:31Z)

## Q5. [escalabilidad] ¿Se contempla escalar horizontalmente ahora?

Todo corre en una máquina y una instancia por servicio.

- A. No se escala ahora: una instancia por servicio, pero se escriben sin estado y con pool de conexiones configurable para poder escalar después
- B. Sí: gateway y `order-service` con dos réplicas en el compose
- C. No se escala y tampoco se prepara; se ajusta cuando haga falta
- X. Other (please specify)

[Answer]: C. No se escala ni se prepara en esta fase: una instancia por servicio, priorizando avanzar a la lógica de negocio y al front. — (**Mode:** chat, 2026-11-11T13:59:31Z)

## Q6. [datos] ¿Hay respaldo o retención definida para esta rebanada?

Los datos viven en un volumen nombrado de PostgreSQL.

- A. Sin respaldo automático; se documenta cómo exportar y restaurar con `pg_dump`
- B. Respaldo automático diario a otro volumen
- C. Sin respaldo y sin documentar
- X. Other (please specify)

[Answer]: A. Sin respaldo automático; se documenta cómo exportar y restaurar con `pg_dump`. — (**Mode:** chat, 2026-11-11T13:59:31Z)

## Q7. [stack] ¿Cómo se sirve el frontend en el contenedor?

El proyecto Angular trae andamiaje de SSR, pero el requisito es un flujo en el navegador.

- A. Build de producción estático servido por nginx; sin SSR
- B. Angular con SSR sobre Node dentro del contenedor
- C. Servidor de desarrollo de Angular (`ng serve`) dentro del contenedor
- X. Other (please specify)

[Answer]: A. Build de producción estático servido por nginx; el SSR se desactiva. — (**Mode:** chat, 2026-11-11T13:59:31Z)

## Q8. [integridad] ¿Qué algoritmo y formato usa el hash del historial?

La revisión de requisitos (R-01) señaló que el historial append-only no se puede verificar sin fijar el algoritmo y la serialización exacta.

- A. SHA-256 sobre una serialización canónica y documentada de los campos, con `previousHash` igual a 64 ceros en la primera fila
- B. SHA-256 sobre el JSON completo de la fila, con `previousHash` nulo en la primera
- C. SHA-512 sobre la misma serialización canónica y génesis de ceros
- X. Other (please specify)

[Answer]: A. SHA-256 sobre una serialización canónica y documentada de los campos, con `previousHash` igual a 64 ceros en la primera fila. — (**Mode:** chat, 2026-11-11T13:59:31Z)

## Consolidated Summary Confirmation

- **Q1 — Rendimiento:** es un MVP, así que no se compromete percentil ni latencia de producción; la verificación es una prueba de humo (registrar y listar con una orden y un usuario, por debajo de 2 s) que queda como línea base. Los usuarios de prueba siguen gehöendo al bloque de autenticación, como ya se decidió en Requirements Analysis (F3).
- **Q2 — Disponibilidad:** arranque local saludable, sin SLO formal; la recuperación se apoya en `restart: unless-stopped` y el SLO llega con staging.
- **Q3 — Seguridad:** solo lo indispensable para que el flujo funcione y el despliegue sea rápido — validación básica de entrada, CORS restringido al origen del frontend, secretos por variables de entorno y ningún puerto de datos publicado; sin rate limiting y sin autenticación en esta rebanada.
- **Q4 — Observabilidad:** logs JSON estructurados con `correlationId`, endpoints de salud de Actuator y management de RabbitMQ; sin métricas ni trazas.
- **Q5 — Escalabilidad:** no se escala ni se prepara en esta fase; una instancia por servicio, con el esfuerzo puesto en lógica de negocio y front.
- **Q6 — Datos:** sin respaldo automático; se documenta cómo exportar y restaurar con `pg_dump`.
- **Q7 — Stack del frontend:** build de producción estático servido por nginx, con el SSR desactivado.
- **Q8 — Integridad del historial:** SHA-256 sobre una serialización canónica y documentada de los campos, con `previousHash` igual a 64 ceros en la primera fila. Esto cierra el hallazgo R-01 de la revisión de requisitos.

Does this all look correct before I generate the NFR artifacts?

- Looks correct
- Request changes

[Answer]: Looks correct
