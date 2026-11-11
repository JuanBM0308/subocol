# Reglas Duras Descubiertas — Taller 360 Subocol

> Solo restricciones que una persona dejó por escrito en `AGENTS.md`,
> `ARCHITECTURE.md` o `CHALLENGE.md`. No es una lista de buenas prácticas
> deducidas del código: si no hay una frase humana que la sostenga, no aparece
> acá. `DECISIONES.md` **no es fuente de estas reglas** —es el documento del
> desarrollador y un entregable al cliente—, así que ninguna regla se apoya en
> él; sí se conserva la obligación de alimentarlo, porque esa obligación está
> escrita en `AGENTS.md` §4 y en `CHALLENGE.md`.
>
> Las citas usan la numeración propia de cada documento. `AGENTS.md` se cita por
> sección (§); `ARCHITECTURE.md` por el identificador de su tabla (TAC, PAT, RST,
> RF), que es el que se puede verificar línea por línea en ese archivo.
>
> Al aprobar la etapa, estas reglas se anexan con fecha en
> `aidlc/spaces/default/memory/project.md`, bajo `## Mandated` y `## Forbidden`.

## Mandated

- ALWAYS mantener la cobertura de pruebas unitarias en 80 % de líneas o más por componente, con JUnit y Mockito en el backend y el umbral equivalente de Vitest en el frontend, y hacer fallar el build por debajo del piso (`AGENTS.md` §5).
- ALWAYS registrar cada cambio de estado de una orden en `order_status_history`, dentro de la misma transacción que el cambio, con `eventId`, `orderId`, `fromStatus`, `toStatus`, `reason`, `changedBy` tomado del JWT, `changedAt` en UTC y `eventHash` encadenado al `previousHash` (`AGENTS.md` §2.B.2).
- ALWAYS publicar los eventos de dominio mediante Transactional Outbox, en la misma transacción del cambio de negocio (`AGENTS.md` §2.D.1 y §4; `ARCHITECTURE.md` TAC-004).
- ALWAYS resolver la reserva de cupos con bloqueo pesimista (`SELECT FOR UPDATE`) o actualización condicional dentro de la transacción de reserva (`AGENTS.md` §2.C; `ARCHITECTURE.md` TAC-003).
- ALWAYS tratar a los consumidores de eventos como idempotentes, registrando los eventos ya procesados para tolerar reentregas (`AGENTS.md` §2.D.2; `ARCHITECTURE.md` TAC-004).
- ALWAYS levantar y desplegar el stack con Podman y Podman Compose a partir de un `compose.yaml` OCI portable, sin utilidades exclusivas de Docker Desktop (`AGENTS.md` §3.1; `ARCHITECTURE.md` RST-02; `CHALLENGE.md` exige que todo levante con un solo comando de compose).
- ALWAYS exponer en `compose.yaml` la red privada `workshop-net` y publicar únicamente los puertos 8080 (frontend), 8081 (gateway) y 15672 (management de RabbitMQ), dejando las bases de datos en contenedores aislados (`AGENTS.md` §3.2).
- ALWAYS crear un usuario y una contraseña por base de datos, con permisos solo sobre su propia base (`AGENTS.md` §2.A; `ARCHITECTURE.md` TAC-005).
- ALWAYS documentar en `DECISIONES.md` toda propuesta rechazada, con la propuesta de la IA, el criterio técnico del rechazo y la solución arquitectónica adoptada, porque es entregable exigido al cliente (`AGENTS.md` §4; `CHALLENGE.md`).
- ALWAYS dejar auditoría y monitoreo de los eventos y registros de las bases de datos, por tablas auxiliares o herramientas open source (`AGENTS.md` §6; `CHALLENGE.md`).

## Forbidden

- NEVER consultar, unir ni replicar datos de la base de otro servicio: sin JOIN ni consultas directas entre dominios, las referencias son identificadores lógicos validados por API REST o por evento (`AGENTS.md` §2.A.4; `ARCHITECTURE.md` PAT-003).
- NEVER compartir una única base de datos relacional entre servicios, ni siquiera como atajo de desarrollo (`AGENTS.md` §2.A y §4).
- NEVER ejecutar `UPDATE` o `DELETE` sobre `order_status_history`: el historial es append-only y un trigger de base de datos o la restricción de permisos revierte el intento (`AGENTS.md` §2.B.3; `ARCHITECTURE.md` PAT-004; `CHALLENGE.md`).
- NEVER notificar al cliente de forma sincrónica ni omitir el outbox: una llamada sincrónica de notificación invalida el diseño (`AGENTS.md` §2.D y §4).
- NEVER emitir el log de WhatsApp para una cancelación derivada del cierre de una orden: si `notifyCustomer=false`, el worker suprime el mensaje (`AGENTS.md` §2.D.3; `CHALLENGE.md`).
- NEVER permitir que un asesor cree manualmente órdenes de origen aseguradora, ni reabrir una orden sin rol `SUPERVISOR` y `reason` no vacío (`AGENTS.md` §2.E; `ARCHITECTURE.md` RF-009, TAC-001; `CHALLENGE.md`).
- NEVER versionar secretos reales ni incrustarlos en la imagen: credenciales, contraseñas y claves van por variables de entorno o por un gestor de secretos (`ARCHITECTURE.md` TAC-005).
- NEVER entregar el backend como un monolito tradicional: el enunciado exige Java con microservicios y cada dominio es dueño de sus datos (`CHALLENGE.md`; `ARCHITECTURE.md` RST-02).
