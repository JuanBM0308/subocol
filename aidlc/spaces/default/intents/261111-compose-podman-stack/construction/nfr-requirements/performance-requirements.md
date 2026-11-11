# Requisitos de rendimiento — Stack Podman y rebanada de órdenes

> Etapa NFR Requirements. Deriva de `requirements.md` (NFR1–NFR9) y de las
> respuestas confirmadas en `nfr-requirements-questions.md`. Contexto de
> referencia: `requirements-analysis/requirements.md`, `AGENTS.md`, `ARCHITECTURE.md`.
> Esta rebanada es un MVP local: el objetivo es demostrar el flujo, no sostener
> carga de producción.

## Alcance

Cubre el camino que el taller va a recorrer en esta fase: registrar una orden y
verla en el listado, atravesando frontend → gateway → `order-service` →
`orders_db`, sobre contenedores locales con Podman.

## Requisitos

| ID | Requisito | Criterio de verificación |
|----|-----------|--------------------------|
| **NFR8.1** | Registrar una orden y listar las registradas responden en menos de 2 segundos con una orden almacenada y un usuario en el navegador, medido en el host del taller con los límites de recursos de NFR8.9 aplicados. | Prueba de humo sobre el stack levantado en ese host: `POST /api/orders` y `GET /api/orders` completados en menos de 2 s cada uno. Es una línea base de no regresión, no un objetivo de servicio. |
| **NFR8.2** | El umbral de 1.000 órdenes respondiendo en menos de 1 segundo queda **diferido** a la rebanada de lógica de negocio y carga real. | No aplica en esta rebanada: no se mide volumen sintético grande a propósito. Se revisa cuando exista el ciclo de estados y el flujo de citas. |
| **NFR8.3** | Tiempos de espera explícitos en cada frontera: gateway → servicio 10 s, servicio → PostgreSQL 5 s, `healthcheck` con intervalo 10 s y timeout 5 s. | Configuración visible en `application.yaml` de cada servicio y en el `healthcheck` del compose; un backend caído produce error en pantalla en menos de 15 s. |
| **NFR8.4** | No se introduce caché en esta rebanada: toda lectura de órdenes va directo a PostgreSQL. | No hay Redis, ni caché de aplicación, ni encabezado `Cache-Control` de persistencia en el gateway. |
| **NFR8.9** | Cada contenedor declara límites de CPU y memoria, para que la medición de NFR8.1 tenga un punto de comparación reproducible: gateway 0,5 CPU y 512 MiB; `order-service` 1 CPU y 1 GiB; `appointment-service` 0,5 CPU y 512 MiB; `notification-worker` 0,25 CPU y 256 MiB; PostgreSQL 1 CPU y 1 GiB; RabbitMQ 0,5 CPU y 512 MiB; frontend (nginx) 0,25 CPU y 128 MiB. | El compose declara `cpus` y `mem_limit` para los siete contenedores con esos valores, y la medición de NFR8.1 se ejecuta con esos límites activos. |

## Notas de diseño

- **Por qué una línea base y no un percentil.** La persona pidió explícitamente
  "acelerar esta fase": comprometer p95 o throughput ahora fijaría números sobre un
  flujo que todavía no existe. El valor de 2 s es una línea base de humo que puede
  verificarse en cualquier máquina del taller, no un objetivo de servicio.
- **Por qué el umbral diferido no se borra.** `requirements.md` NFR8 fija 1.000 órdenes
  en menos de 1 segundo. El diferimiento queda explícito para que la etapa que
  implemente el ciclo de estados lo recupere en lugar de perderlo de vista.
- **Sin caché ni réplicas.** Añadir cualquiera de las dos cosas ahora es optimización
  prematura: no hay carga que lo justifique y ambas aumentan la superficie de
  operación local, que es lo que hoy más cuesta.

## Diferido

| ID | Motivo | Cuándo se retoma |
|----|--------|------------------|
| NFR8.2 | El volumen de órdenes y la concurrencia real todavía no existen; el flujo es un MVP de una orden. | Rebanada de lógica de negocio, cuando el taller tenga uso real de la herramienta. |
