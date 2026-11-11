# Contrato de dominio — Taller 360 Subocol

> Complemento de `docs/mvp-contract.md`. Define el ciclo de vida de la orden, las
> citas, el bus de eventos y el log de WhatsApp simulado, para que los tres
> servicios hablen el mismo idioma.

## 1. Estados de la orden

Catalogo propuesto en `ARCHITECTURE.md` (RST-08: sujeto a validacion funcional):

```
RECIBIDA -> EN_DIAGNOSTICO -> ESPERA_APROBACION -> EN_REPARACION -> LISTA_PARA_ENTREGA -> CERRADA
CERRADA  -> REABIERTA   (solo con rol SUPERVISOR y motivo no vacio)
REABIERTA -> EN_DIAGNOSTICO (sigue el ciclo funcional aprobado)
```

Reglas:

- Solo se acepta una transicion desde la tabla de transiciones; cualquier otra
  devuelve `409 Conflict` con `ProblemDetail`.
- Cada transicion escribe una fila en `order_status_history` **en la misma
  transaccion** que el cambio de estado: `eventId`, `orderId`, `fromStatus`,
  `toStatus`, `reason`, `changedBy`, `changedAt` (UTC) y el par
  `eventHash`/`previousHash` con la serializacion canonica ya fijada.
- El origen de la orden lo fija el servidor en `ADVISOR`. Un asesor **no** puede
  crear una orden de origen aseguradora (regla afirmada del taller).

### API de transiciones

`POST /api/orders/{orderId}/transitions`

```json
{ "toStatus": "EN_DIAGNOSTICO", "reason": "Revision inicial" }
```

- `reason` es obligatoria para reabrir (`CERRADA -> REABIERTA`) y vacia en el resto.
- Respuesta `200` con la orden actualizada y su historial.
- `409` si la transicion no es valida, `403` si reabre sin rol `SUPERVISOR`,
  `400` si reabre sin motivo.

### Actor y rol

Mientras no exista autenticacion, el servidor toma el actor y su rol de la
configuracion (`app.actor`, `app.actor-role`), nunca del cuerpo de la peticion.
El compose los define como `dev-advisor` / `SUPERVISOR`. Cuando llegue el bloque
de identidad, esos dos valores pasan a salir del token (PAT-007) y el resto del
codigo no cambia.

## 2. Citas y cupos

Entidades del `appointment-service` (base `appointments_db`, su propio rol):

- `advisor_slot(id, advisor_id, starts_at, ends_at, capacity, reserved)`:
  la capacidad por asesor y franja.
- `appointment(id, order_id, advisor_id, slot_id, type, status, contact_name,
  contact_phone, created_at, cancelled_at, cancel_reason)`:
  `type` = `PICKUP` | `DELIVERY`; `status` = `PENDING` | `CANCELLED` |
  `COMPLETED`.

Regla de concurrencia (TAC-003, PAT-007): la reserva se resuelve con
`SELECT ... FOR UPDATE` sobre la fila del slot dentro de la transaccion, y el
contador `reserved` sube solo si hay capacidad. Dos reservas simultaneas sobre
el mismo slot no pueden pasarse entre si.

### API

- `GET /api/appointments/slots?advisorId=&date=` → franjas con su capacidad y
  sus reservas.
- `POST /api/appointments` → registra la cita.
  ```json
  { "orderId": "...", "advisorId": "asesor-1", "slotId": "...",
    "type": "PICKUP", "contactName": "Ana Ruiz", "contactPhone": "3001234567" }
  ```
  Respuesta `201`. `409` si el slot se lleno mientras se reservaba.
- `GET /api/appointments?orderId=` → citas de una orden.
- `GET /api/appointments/advisors` → lista de asesores con sus franjas.

El `orderId` es una referencia logica: el servicio **no** consulta la base de
ordenes; valida el id contra la API del `order-service` (o lo deja pasar si la
orden no responde, dejando la traza del error).

## 3. Bus de eventos

- Exchange: `subocol.events` (tipo `topic`, durable).
- Routing key: el nombre del evento (`OrderClosed`, `AppointmentScheduled`,
  `AppointmentCancelled`).
- Sobre (envelope) en JSON:
  ```json
  { "eventId": "uuid", "eventType": "OrderClosed", "occurredAt": "ISO-8601 UTC",
    "aggregateId": "orderId", "payload": { ... } }
  ```
- Publicacion: **Transactional Outbox**. Cada servicio tiene su tabla
  `outbox_events` en su propia base y la escribe en la misma transaccion del
  cambio de negocio. Un publicador periodico (cada 1 s) toma las filas sin
  `published_at` y las publica; al confirmar la publicacion marca la fila.
  Si el broker no esta disponible, la fila queda pendiente y se reintenta.

### Eventos

| Evento | Quien lo publica | Payload |
|--------|------------------|---------|
| `OrderClosed` | order-service | `{ "orderId": "...", "closedBy": "...", "closedAt": "ISO-8601 UTC" }` |
| `AppointmentScheduled` | appointment-service | `{ "appointmentId": "...", "orderId": "...", "advisorId": "...", "type": "PICKUP", "contactName": "...", "contactPhone": "...", "startsAt": "ISO-8601 UTC" }` |
| `AppointmentCancelled` | appointment-service | `{ "appointmentId": "...", "orderId": "...", "advisorId": "...", "type": "...", "contactName": "...", "contactPhone": "...", "startsAt": "...", "cancelReason": "ORDER_CLOSED", "notifyCustomer": false }` |

Regla dura: en la cancelacion **automatica** por cierre de orden,
`notifyCustomer` es `false` y el worker **no** escribe el mensaje. En la
cancelacion **manual** es `true` y el worker si lo escribe.

## 4. Consumidores idempotentes

- `appointment-service` consume `OrderClosed`: busca las citas `PENDING` de esa
  orden, las pasa a `CANCELLED` con `cancel_reason = 'ORDER_CLOSED'` y
  `notify_customer = false`, y publica `AppointmentCancelled`.
  Idempotencia: tabla `processed_events(event_id pk, consumer_name, processed_at)`.
  Un evento ya procesado se ignora.
- `notification-worker` consume `AppointmentScheduled` y `AppointmentCancelled`
  y escribe el log de WhatsApp en `stdout`. Es el unico componente sin base de
  datos, asi que su idempotencia es en memoria (conjunto de `eventId` vistos)
  y queda documentada como tal.

## 5. Log de WhatsApp simulado

Una linea por mensaje, en `stdout` del `notification-worker`, con este formato:

```
WHATSAPP | to=3001234567 | type=APPOINTMENT_SCHEDULED | orderId=... | appointmentId=... | date=2026-11-12T09:00:00Z | Hola Ana Ruiz, tu cita para la entrega del vehiculo quedo agendada el 12 de noviembre a las 09:00.
WHATSAPP | to=3001234567 | type=APPOINTMENT_CANCELLED | orderId=... | appointmentId=... | date=... | Hola Ana Ruiz, tu cita del 12 de noviembre a las 09:00 fue cancelada.
```

Y **nada** cuando `notifyCustomer=false`.

## 6. Estado inicial de los datos

- `advisor_slot`: el compose siembra tres asesores con franjas de la manana y
  de la tarde, con capacidad configurable, desde los scripts de inicializacion
  de `appointments_db`.
- No se siembran ordenes de ejemplo en el codigo: el flujo se prueba desde la
  interfaz.
