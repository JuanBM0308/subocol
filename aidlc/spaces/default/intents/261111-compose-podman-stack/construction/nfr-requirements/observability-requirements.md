# Requisitos de observabilidad — Stack Podman y rebanada de órdenes

> Etapa NFR Requirements. Deriva de `requirements.md` (NFR5) y de las respuestas
> confirmadas en `nfr-requirements-questions.md` (Q4). Contexto de referencia:
> `requirements-analysis/requirements.md`, `AGENTS.md` §6, `ARCHITECTURE.md`
> (TAC-007, PAT-004).
> La persona eligió la opción mínima: logs estructurados con `correlationId`,
> endpoints de salud y management del broker. Sin métricas ni trazas.

## Alcance

Define qué se puede observar del stack mientras corre y qué debe quedar registrado
en cada petición. No incluye métricas de negocio ni trazas distribuidas: en una
instalación local de un taller, el valor está en poder leer qué pasó en una
petición concreta y en saber si un contenedor está sano.

## Requisitos

| ID | Requisito | Criterio de verificación |
|----|-----------|--------------------------|
| **NFR5.1** | Cada servicio emite logs en JSON estructurado con un `correlationId` en cada línea. | Una petición al gateway produce logs JSON con el mismo `correlationId` en el gateway, en el servicio y en el acceso a la base. |
| **NFR5.2** | El gateway propaga o genera el `correlationId` (cabecera `X-Correlation-Id`) y lo mantiene en toda la cadena. | La petición llega al servicio con la cabecera y el servicio la reutiliza en su log. |
| **NFR5.3** | Cada servicio y el gateway exponen su estado de salud con Actuator (`/actuator/health`), incluyendo dependencias relevantes. | `GET /actuator/health` responde 200 con estado `UP` y reporta el estado de la base y del broker. |
| **NFR5.4** | El `healthcheck` del compose consulta el endpoint de salud, no un chequeo superficial de proceso. | El `healthcheck` del compose usa HTTP contra el endpoint de salud; un servicio con la base caída reporta `unhealthy`. |
| **NFR5.5** | RabbitMQ es observable por su interfaz de management en 15672, que es la única vista de la salud del broker. | La interfaz de management responde y muestra la cola y los consumidores del stack. |
| **NFR5.6** | Los logs no exponen datos personales del cliente ni del vehículo sin enmascarar; se enmascaran identificadores y contactos. | Revisión de los logs del flujo de registro: ningún campo de identificación del cliente o del vehículo aparece en claro. |
| **NFR5.7** | El frontend no implementa observabilidad propia: la fuente de verdad del diagnóstico es el log del backend. | No hay consola de logs ni endpoint de diagnóstico en el frontend; la documentación señala dónde mirar. |
| **NFR5.8** | La auditoría de los eventos de la base queda cubierta en esta rebanada por el registro append-only `order_status_history` con su cadena de hash, y por el log propio del servidor PostgreSQL, que queda accesible desde `podman compose logs`. | Cada creación de orden deja su fila de auditoría verificable (ver NFR4.1 y NFR4.2) y el log del servidor de PostgreSQL registra los eventos de conexión y error de la sesión de verificación. |

## Notas de diseño

- **El `correlationId` es la costura clave.** Con tres procesos en juego (gateway,
  servicio, base), un identificador que viaje por la petición es lo que permite
  reconstruir una petición completa sin perder la correlación en el registro de cada
  contenedor.
- **Salud profunda, no superficial.** Un chequeo de proceso (`is the port open?`)
  declara sano un servicio cuya base no responde, que es exactamente el fallo que
  más tiempo cuesta diagnosticar en un entorno local. Actuator con estado de
  dependencias cubre ese caso.
- **Por qué no métricas ni trazas.** Prometheus, Micrometer y OpenTelemetry suman
  configuración, un contenedor más o un Collectors, y un presupuesto de operación
  que un taller de un día no tiene. La decisión queda registrada y es reversible:
  Actuator ya expone las métricas que Micrometer soporta cuando se quiera activar.

## Diferido

| ID | Motivo | Cuándo se retoma |
|----|--------|------------------|
| Métricas Micrometer/Prometheus | No hay un ambiente donde recolectarlas ni un operador que las mire; el valor marginal en local es bajo. | Cuando exista staging con despliegue al integrar y varios ambientes que comparar. |
| Trazas distribuidas OpenTelemetry | La instrumentación de trazas cuesta tiempo de implementación que hoy debe ir a la lógica de negocio. | Con la llegada del segundo consumidor (citas y notificaciones), donde el camino asíncrono sí se vuelve difícil de seguir con logs. |
| Tablas auxiliares de auditoría de cambios de las bases | En esta rebanada el único evento de dominio es la creación de una orden, y ya queda auditado en `order_status_history` con cadena de hash (NFR5.8). Una auditoría genérica de cambios de tablas no tiene eventos que auditar todavía. | Rebanada de eventos de dominio (outbox transaccional y cierre de orden), donde aparecen los eventos que la auditoría genérica debe registrar. |
