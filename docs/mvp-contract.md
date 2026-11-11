# Contrato del MVP — Taller 360 Subocol

> Contrato de integración entre los componentes del corte vertical que se entrega
> el 11 de noviembre. Todo lo que no aparece aquí queda fuera del MVP.

## Objetivo del MVP

Un solo comando (`podman compose up --build`) deja el stack arriba y el flujo
**registrar orden → ver listado → ver detalle** funcionando en
`http://localhost:8080`, con datos reales en `orders_db`.

## Componentes y puertos

| Componente | Puerto interno | Puerto publicado | Imagen / contexto |
|------------|----------------|------------------|-------------------|
| frontend (nginx + Angular) | 80 | **8080** | contexto `subocol-frontend/` |
| gateway | 8081 | **8081** | contexto `subocol-gateway/` |
| `order-service` | 8082 | — | contexto `subocol-order-service/` |
| `appointment-service` | 8083 | — | contexto `subocol-appointment-service/` |
| `notification-worker` | 8084 (solo actuator) | — | contexto `subocol-notification-worker/` |
| PostgreSQL 16 | 5432 | — | oficial, volumen nombrado |
| RabbitMQ | 5672 / 15672 | **15672** (management) | oficial, volumen nombrado |

Red: `workshop-net` (privada). Nadie publica 5432 ni 5672.

## API de órdenes (`order-service`, expuesto por el gateway en `/api/orders`)

### `POST /api/orders`

```json
{
  "customerName": "Ana Ruiz",
  "vehiclePlate": "ABC123",
  "vehicleDescription": "Toyota Corolla 2018",
  "insurerCode": null
}
```

Respuesta `201`:

```json
{ "orderId": "3f9d…", "status": "RECIBIDA", "createdAt": "2026-11-11T15:04:05.123Z" }
```

Validación (400 con `ProblemDetail`): `customerName` obligatorio máx. 120,
`vehiclePlate` obligatorio máx. 16, `vehicleDescription` obligatorio máx. 2000,
`insurerCode` opcional máx. 32. El origen lo fija el servidor en `ADVISOR`; el
cliente no puede elegirlo.

### `GET /api/orders?page=0&size=20`

```json
{
  "content": [
    { "orderId": "3f9d…", "customerName": "Ana Ruiz", "vehiclePlate": "ABC123",
      "vehicleDescription": "Toyota Corolla 2018", "insurerCode": null,
      "status": "RECIBIDA", "createdAt": "2026-11-11T15:04:05.123Z" }
  ],
  "page": 0, "size": 20, "totalElements": 1
}
```

### `GET /api/orders/{orderId}`

Igual que un elemento del listado más `history`:

```json
{
  "orderId": "3f9d…", "customerName": "Ana Ruiz", "vehiclePlate": "ABC123",
  "vehicleDescription": "Toyota Corolla 2018", "insurerCode": null,
  "status": "RECIBIDA", "createdAt": "2026-11-11T15:04:05.123Z",
  "history": [
    { "eventId": "9c1a…", "fromStatus": null, "toStatus": "RECIBIDA",
      "reason": "Orden registrada", "changedBy": "dev-advisor",
      "changedAt": "2026-11-11T15:04:05.123Z",
      "eventHash": "9f2b…", "previousHash": "0000000000000000000000000000000000000000000000000000000000000000" }
  ]
}
```

## Base de datos

Base `orders_db` con dos roles:

| Rol | Uso | Permisos |
|-----|-----|-----------|
| `orders_owner` | migraciones Flyway | dueño del esquema y de los objetos |
| `orders_user` | aplicación | `SELECT`/`INSERT` en `order_status_history`; `SELECT`/`INSERT`/`UPDATE` en `orders`; sin `UPDATE`/`DELETE`/`TRUNCATE` sobre el historial |

Esquema (lo crea Flyway `V1__create_order_tables.sql`, no los scripts de init):

- `orders(id uuid pk, customer_name varchar(120) not null, vehicle_plate varchar(16) not null, vehicle_description varchar(2000) not null, insurer_code varchar(32) null, status varchar(32) not null, created_at timestamptz not null)`
- `order_status_history(id uuid pk, event_id uuid not null unique, order_id uuid not null references orders(id), from_status varchar(32) null, to_status varchar(32) not null, reason varchar(500) null, changed_by varchar(120) not null, changed_at timestamptz not null, event_hash char(64) not null, previous_hash char(64) not null)`
- Trigger `BEFORE UPDATE OR DELETE` sobre `order_status_history` que lanza excepción.
- Índices: `orders (created_at DESC)`, `order_status_history (order_id, changed_at)`.

### Hash canónico del historial

`eventHash` = SHA-256 en hexadecimal minúsculas sobre la concatenación, en UTF-8, de
los ocho campos en este orden — `eventId`, `orderId`, `fromStatus`, `toStatus`,
`reason`, `changedBy`, `changedAt`, `previousHash` — donde cada campo se escribe
como `<longitudEnBytes>:<valor>` y los fragmentos se unen con `|`. Valor nulo: `~`.
Valor vacío: `0:`. Los UUID en forma textual RFC 4122 en minúsculas. `changedAt` en
ISO-8601 UTC con milisegundos `yyyy-MM-dd'T'HH:mm:ss.SSS'Z'`. La primera fila de
cada orden usa `previousHash` = 64 ceros.

## Configuración por variables de entorno

| Variable | Dónde | Valor por defecto |
|----------|-------|-------------------|
| `ORDERS_DB_PASSWORD`, `ORDERS_OWNER_PASSWORD` | scripts de init | valores de ejemplo |
| `APPOINTMENTS_DB_PASSWORD`, `APPOINTMENT_OWNER_PASSWORD` | scripts de init | valores de ejemplo |
| `RABBITMQ_DEFAULT_USER`, `RABBITMQ_DEFAULT_PASS`, `RABBITMQ_DEFAULT_VHOST` | compose | `workshop` / ejemplo / `workshop` |
| `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | `order-service` | — |
| `SPRING_FLYWAY_USER`, `SPRING_FLYWAY_PASSWORD` | `order-service` | — |
| `APP_ACTOR` | `order-service` | `dev-advisor` (identidad de desarrollo hasta que exista JWT) |

## Reglas transversales

- Logs estructurados JSON con `correlationId` en cada línea.
- `X-Correlation-Id`: el gateway lo genera si no viene y lo propaga; cada servicio lo
  deja en el MDC.
- `GET /actuator/health` responde 200 con el estado de la base; sin puerto de
  management aparte.
- El frontend llama a rutas relativas `/api/...`; nginx hace de proxy inverso hacia
  el gateway, así que el navegador nunca ve otro origen.
- Cero secretos reales versionados; `.env.example` documenta cada variable.
