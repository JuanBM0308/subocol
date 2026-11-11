# Diseño de rendimiento — Stack Podman y rebanada de órdenes

> Etapa NFR Design. Consume `nfr-requirements/performance-requirements.md`,
> `nfr-requirements/reliability-requirements.md` y
> `nfr-requirements/tech-stack-decisions.md`. Decisiones de diseño tomadas en
> `nfr-design-questions.md` (Q1, Q5). Describe patrones y decisiones, no
> implementación final.

## Objetivos que resuelve este diseño

- **NFR8.1** — línea base de humo de 2 s para registrar y listar, medida en el host
  del taller con los límites de NFR8.9.
- **NFR8.3** — tiempos de espera explícitos en cada frontera.
- **NFR8.4** — sin caché en esta rebanada.
- **NFR8.9** — límites de CPU y memoria por contenedor.

## Presupuesto de latencia por tramo

El presupuesto se reparte para que ningún tramo consuma por sí solo la mayor parte
del 2 s:

| Tramo | Presupuesto | Mecanismo |
|-------|-------------|-----------|
| nginx → gateway (loopback de red) | 50 ms | Proxy inverso, conexión persistente al upstream |
| gateway → `order-service` | 100 ms | Balanceo round-robin de Spring Cloud Gateway, una sola instancia |
| `order-service` → PostgreSQL | 300 ms | Consulta indexada, pool HikariCP ya caliente |
| Consulta, serialización y red de vuelta | 200 ms | DTOs planos, sin carga diferida de relaciones |
| Renderizado inicial del frontend | 500 ms | Build estático con assets versionados |
| **Margen de la línea base** | **850 ms** | Holgura para el resto del camino |

La suma deja el flujo dentro del 2 s de NFR8.1 con margen paraVariation del host. La
verificación es una prueba de humo que mide el tramo completo desde el navegador,
no por tramo.

## Pool de conexiones

**Decisión (Q5):** los valores por defecto de Spring Boot, sin configuración propia de
HikariCP.

- Pool por servicio: máximo 10 conexiones, mínimo igual al máximo (HikariCP mantiene
  las conexiones abiertas), timeout de conexión de 30 s por defecto.
- Con dos servicios que usan base, el máximo teórico es 20 conexiones de aplicación
  más las de administración del contenedor PostgreSQL.
- Con `mem_limit` de 1 GiB para PostgreSQL y 512 MiB para los servicios, el consumo
  del pool es del orden de unos pocos MiB: no es el recurso que aprieta.
- **Consecuencia asumida:** si el host del taller llegara a tener varios servicios
  sobre la misma base, el pool por defecto sería lo primero que habría que ajustar. Se
  acepta para esta rebanada y queda anotado para la de lógica de negocio.

## Sin caché: cómo se compensa

NFR8.4 exige no introducir caché. El coste se compensa con tres medidas:

1. **Índices creados por la migración Flyway** (no por la aplicación):
   - `orders (created_at DESC)` para el listado paginado más reciente.
   - `orders (status)` para el filtro por estado.
   - `order_status_history (order_id, changed_at)` para el detalle de una orden.
2. **Paginación en el listado**: página de 20 por defecto, máximo 100, con
   `OFFSET/LIMIT` sobre el índice de fecha. El listado nunca carga el historial.
3. **Lecturas sin N+1**: el detalle carga la orden y su historial en dos consultas
   explícitas, nunca una por fila.

No hay caché de aplicación, ni CDN de datos, ni `Cache-Control` de persistencia en el
gateway. Los assets estáticos sí se cachean en nginx por hash de contenido: eso es
cacheo de archivos, no de datos de negocio.

## Recursos por contenedor y JVM

NFR8.9 fija los límites de CPU y memoria por contenedor. El diseño los aplica así:

| Contenedor | CPU | Memoria | Ajuste de JVM |
|------------|-----|---------|----------------|
| gateway | 0,5 | 512 MiB | `-XX:MaxRAMPercentage=70` |
| `order-service` | 1 | 1 GiB | `-XX:MaxRAMPercentage=70` |
| `appointment-service` | 0,5 | 512 MiB | `-XX:MaxRAMPercentage=70` |
| `notification-worker` | 0,25 | 256 MiB | `-XX:MaxRAMPercentage=70` |
| PostgreSQL | 1 | 1 GiB | `shared_buffers=256MB`, `work_mem=4MB` |
| RabbitMQ | 0,5 | 512 MiB | sin ajuste (consumo bajo en esta rebanada) |
| frontend (nginx) | 0,25 | 128 MiB | no aplica |

`MaxRAMPercentage=70` deja el 30 % del límite para la metainformación del heap, el
hilo de GC y el stack de hilos: con `mem_limit` en el compose, la JVM debe conocer el
límite del contenedor para no pasarse.

Los ajustes de PostgreSQL (`shared_buffers`, `work_mem`) son los valores por defecto
de la imagen oficial reducidos a la mitad, coherentes con el límite de 1 GiB.

## Procesamiento asíncrono

En esta rebanada no lo hay, y es deliberado: el único evento de dominio es la
creación de una orden, que debe responder de forma síncrona para que el frontend
muestre el resultado. RabbitMQ queda arriba y saludable pero sin carga
(`nfr-requirements/observability-requirements.md`, NFR5.5). El Transactional Outbox
y el consumo idempotente llegan con la rebanada de eventos de dominio; el diseño deja
el hueco estructural: los servicios serán stateless y publicarán por outbox, sin
cambios en el frontend ni en el gateway.

## Tiempos de espera (NFR8.3)

| Frontera | Timeout | Dónde se configura |
|----------|---------|---------------------|
| gateway → servicio | 10 s | `spring.cloud.gateway.httpclient.connect-timeout` y `response-timeout` |
| servicio → PostgreSQL (conexión) | 5 s | `spring.datasource.hikari.connection-timeout` |
| lectura con timeout | 10 s | `spring.datasource.hikari.validation-timeout` y query timeout de la conexión |
| `healthcheck` del compose | timeout 5 s, intervalo 10 s, reintentos 5 | bloque `healthcheck` de cada servicio |

## Verificación

1. Prueba de humo con el stack levantado: `POST /api/orders` y `GET /api/orders` por
   debajo de 2 s cada uno (NFR8.1), en el host del taller con los límites de NFR8.9.
2. `podman compose ps` muestra los contenedores dentro de sus límites; ningún
   contenedor aparece en estado de memoria restringida.
3. Los tiempos de espera se verifican levantando el stack con la base detenida y
   comprobando que la respuesta llega en menos de 15 s con error claro (NFR8.3).

## Riesgos

| Riesgo | Impacto | Mitigación en el diseño |
|--------|---------|-------------------------|
| El host del taller es más lento que la línea base | La medición de 2 s falla | La línea base es de no regresión, no de servicio; si el host no da la talla, se registra la cifra real y se revisa el presupuesto por tramo |
| El pool por defecto (10) resulta alto si se suman servicios | Contención de conexiones | Límites de memoria por contenedor y `shared_buffers` acotado; el ajuste del pool queda anotado para la rebanada de lógica de negocio |
| `MaxRAMPercentage` mal calculado | OOM kill del contenedor | 70 % deja 30 % de holgura; el reinicio automático (ver `reliability-design.md`) recupera el servicio |
