# Project-Level Rules

> Project-specific specialisation and corrections. Loaded after `org.md` and
> `team.md` as strict-additive guidance; contradictions with broader policy
> are rejected. Populated by practices-discovery and the self-learning loop.
>
> Use sparingly: most teams don't need a project layer. Reach for it
> only when this specific project needs stable, durable guidance beyond the
> team practice (for example, package-specific release checks or an additional
> regression suite for a legacy component).

## Way of Working

<!-- Project-specific specialisation. Example: -->
<!-- This monorepo requires package-scoped branch names and a package owner -->
<!-- review in addition to the team's normal merge policy. -->

## Walking Skeleton

<!-- Project-specific specialisation. Example: -->
<!-- The walking skeleton must exercise the legacy service adapter as well -->
<!-- as the new service boundary. -->

## Testing Posture

<!-- Project-specific specialisation. -->

## Guard Policy

<!-- Project-specific. Mode: strict, relaxed, or off. Strict here holds for every intent, whatever one person asks in chat; relaxed or off here applies to every intent whose policy came from its scope, unless the person sets another; changing this line changes it. A section under the retired Change Control heading, written by an earlier release, is still read. -->

## Deployment

<!-- Project-specific specialisation. -->

## Code Style

<!-- Project-specific specialisation. -->

## Tech Stack

<!-- Technology choices locked for this project. -->

## Decided

<!-- Decisions made in earlier stages that should not be re-asked. -->
<!-- Format: DECIDED: [decision] (Stage [slug], [date]) -->

## Scope Overrides

<!-- Custom scope rules for this project. -->

## Forbidden

<!-- Populated by practices-discovery affirmation gate. -->
<!-- Format: NEVER [behavior] (affirmed [date]) -->
<!-- Example: NEVER throw exceptions across service layer boundaries (affirmed 2026-05-17) -->

- NEVER consultar, unir ni replicar datos de la base de otro servicio: sin JOIN ni consultas directas entre dominios, las referencias son identificadores lógicos validados por API REST o por evento (`AGENTS.md` §2.A.4; `ARCHITECTURE.md` PAT-003). (affirmed 2026-11-11)

- NEVER compartir una única base de datos relacional entre servicios, ni siquiera como atajo de desarrollo (`AGENTS.md` §2.A y §4). (affirmed 2026-11-11)

- NEVER ejecutar `UPDATE` o `DELETE` sobre `order_status_history`: el historial es append-only y un trigger de base de datos o la restricción de permisos revierte el intento (`AGENTS.md` §2.B.3; `ARCHITECTURE.md` PAT-004; `CHALLENGE.md`). (affirmed 2026-11-11)

- NEVER notificar al cliente de forma sincrónica ni omitir el outbox: una llamada sincrónica de notificación invalida el diseño (`AGENTS.md` §2.D y §4). (affirmed 2026-11-11)

- NEVER emitir el log de WhatsApp para una cancelación derivada del cierre de una orden: si `notifyCustomer=false`, el worker suprime el mensaje (`AGENTS.md` §2.D.3; `CHALLENGE.md`). (affirmed 2026-11-11)

- NEVER permitir que un asesor cree manualmente órdenes de origen aseguradora, ni reabrir una orden sin rol `SUPERVISOR` y `reason` no vacío (`AGENTS.md` §2.E; `ARCHITECTURE.md` RF-009, TAC-001; `CHALLENGE.md`). (affirmed 2026-11-11)

- NEVER versionar secretos reales ni incrustarlos en la imagen: credenciales, contraseñas y claves van por variables de entorno o por un gestor de secretos (`ARCHITECTURE.md` TAC-005). (affirmed 2026-11-11)

- NEVER entregar el backend como un monolito tradicional: el enunciado exige Java con microservicios y cada dominio es dueño de sus datos (`CHALLENGE.md`; `ARCHITECTURE.md` RST-02). (affirmed 2026-11-11)

## Mandated

<!-- Populated by practices-discovery affirmation gate. -->
<!-- Format: ALWAYS [behavior] (affirmed [date]) -->
<!-- Example: ALWAYS use Result<T,E> for fallible operations in service layer (affirmed 2026-05-17) -->

- ALWAYS mantener la cobertura de pruebas unitarias en 80 % de líneas o más por componente, con JUnit y Mockito en el backend y el umbral equivalente de Vitest en el frontend, y hacer fallar el build por debajo del piso (`AGENTS.md` §5). (affirmed 2026-11-11)

- ALWAYS registrar cada cambio de estado de una orden en `order_status_history`, dentro de la misma transacción que el cambio, con `eventId`, `orderId`, `fromStatus`, `toStatus`, `reason`, `changedBy` tomado del JWT, `changedAt` en UTC y `eventHash` encadenado al `previousHash` (`AGENTS.md` §2.B.2). (affirmed 2026-11-11)

- ALWAYS publicar los eventos de dominio mediante Transactional Outbox, en la misma transacción del cambio de negocio (`AGENTS.md` §2.D.1 y §4; `ARCHITECTURE.md` TAC-004). (affirmed 2026-11-11)

- ALWAYS resolver la reserva de cupos con bloqueo pesimista (`SELECT FOR UPDATE`) o actualización condicional dentro de la transacción de reserva (`AGENTS.md` §2.C; `ARCHITECTURE.md` TAC-003). (affirmed 2026-11-11)

- ALWAYS tratar a los consumidores de eventos como idempotentes, registrando los eventos ya procesados para tolerar reentregas (`AGENTS.md` §2.D.2; `ARCHITECTURE.md` TAC-004). (affirmed 2026-11-11)

- ALWAYS levantar y desplegar el stack con Podman y Podman Compose a partir de un `compose.yaml` OCI portable, sin utilidades exclusivas de Docker Desktop (`AGENTS.md` §3.1; `ARCHITECTURE.md` RST-02; `CHALLENGE.md` exige que todo levante con un solo comando de compose). (affirmed 2026-11-11)

- ALWAYS exponer en `compose.yaml` la red privada `workshop-net` y publicar únicamente los puertos 8080 (frontend), 8081 (gateway) y 15672 (management de RabbitMQ), dejando las bases de datos en contenedores aislados (`AGENTS.md` §3.2). (affirmed 2026-11-11)

- ALWAYS crear un usuario y una contraseña por base de datos, con permisos solo sobre su propia base (`AGENTS.md` §2.A; `ARCHITECTURE.md` TAC-005). (affirmed 2026-11-11)

- ALWAYS documentar en `DECISIONES.md` toda propuesta rechazada, con la propuesta de la IA, el criterio técnico del rechazo y la solución arquitectónica adoptada, porque es entregable exigido al cliente (`AGENTS.md` §4; `CHALLENGE.md`). (affirmed 2026-11-11)

- ALWAYS dejar auditoría y monitoreo de los eventos y registros de las bases de datos, por tablas auxiliares o herramientas open source (`AGENTS.md` §6; `CHALLENGE.md`). (affirmed 2026-11-11)

## Corrections

<!-- Project-specific corrections from human feedback. -->
<!-- Format: NEVER/ALWAYS [behavior] (learned [date]) -->
