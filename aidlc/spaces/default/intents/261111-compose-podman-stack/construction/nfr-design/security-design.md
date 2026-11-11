# Diseño de seguridad — Stack Podman y rebanada de órdenes

> Etapa NFR Design. Consume `nfr-requirements/security-requirements.md`,
> `nfr-requirements/observability-requirements.md` y
> `nfr-requirements/tech-stack-decisions.md`. Decisiones tomadas en
> `nfr-design-questions.md` (Q1, Q2, Q6, Q7, Q8). Describe patrones y decisiones, no
> implementación final.

## Objetivo

Que la rebanada sea segura por construcción en lo que le corresponde: aislamiento real
entre dominios, historial inmutable frente a la propia aplicación, ninguna credencial
real versionada ni incrustada, y ninguna superficie de datos expuesta al host.

## 1. Modelo de roles en base de datos (Q7)

**Decisión:** dos roles por base.

| Rol | Base | Puede | No puede |
|-----|------|-------|----------|
| `orders_owner` | `orders_db` | Dueño del esquema y de los objetos; ejecuta migraciones Flyway | Es usado solo por el proceso de migración, nunca por la aplicación en runtime |
| `orders_user` | `orders_db` | `CONNECT`, `SELECT`, `INSERT`, `UPDATE` sobre las tablas de negocio; `SELECT` e `INSERT` sobre `order_status_history` | `UPDATE`, `DELETE`, `TRUNCATE` sobre `order_status_history`; `DROP` de objetos; cualquier acceso a `appointments_db` |
| `appointment_owner` | `appointments_db` | Dueño del esquema; ejecuta migraciones Flyway | — |
| `appointment_user` | `appointments_db` | Igual que `orders_user` sobre su propia base | Cualquier acceso a `orders_db` |

**Cómo se materializa:**

- Los scripts de `docker-entrypoint-initdb.d` crean las dos bases y los cuatro roles,
  con los permisos de la tabla anterior, y no crean tablas (el esquema es de Flyway).
- Flyway corre con el rol propietario (`orders_owner` / `appointment_owner`), que es
  el único que puede crear tablas, índices, triggers y funciones.
- La aplicación corre con el rol de aplicación (`orders_user` /
  `appointment_user`), que solo hace DML.
- El trigger append-only se crea en la migración y se apoya en `REVOKE UPDATE,
  DELETE ON order_status_history FROM orders_user`: la negativa no depende del
  código de la aplicación, sino de los permisos del rol.

**Consecuencia sobre los requisitos:** la regla del taller —un rol de aplicación por
base, sin acceso cruzado— se mantiene intacta; lo que cambia es que cada base tiene
además un rol propietario que solo migra. La redacción de FR3.1 («los dos roles») se
amplía a cuatro roles, todos confinados a su propia base.

**Por qué no un solo rol:** con un rol único, la aplicación podía ejecutar `UPDATE`
sobre el historial o soltar el trigger; la inmutabilidad quedaría en manos del
código, que es exactamente lo que `AGENTS.md` §2.B.3 prohíbe.

## 2. Autenticación y autorización

No hay autenticación en esta rebanada (fuera de alcance, bloque de identidad). El
diseño deja el hueco listo:

- El campo `changedBy` se toma de una propiedad de configuración `app.actor` cuyo
  valor por defecto en local es un identificador fijo y documentado
  (`dev-advisor`). No viene del cliente ni de una petición: el servidor no acepta el
  actor desde el cuerpo de la petición.
- Cuando llegue el bloque de identidad, ese valor pasa a derivarse del claim del JWT
  y la configuración deja de tener el valor por defecto.
- El gateway no expone endpoints administrativos (NFR7.5): solo enruta `/api/orders/**`
  y su endpoint de salud.

## 3. Validación de entrada (NFR7.2)

- **En el borde y en el servicio.** DTOs de entrada con Bean Validation y las
  longitudes máximas fijadas por el requisito: razón social 120, descripción 2.000,
  matrícula 16, teléfono 32, correo 120, código de aseguradora 32, identificadores 36.
- **Longitudes de columna.** Las migraciones Flyway declaran las columnas con esos
  mismos límites, de modo que la base y la validación no puedan divergir.
- **Errores sin detalle interno.** Las respuestas de error usan `ProblemDetail` con un
  código estable y un mensaje accionable; nunca incluyen nombre de clase, traza de
  pila ni detalle de SQL.
- **Origen permitido.** El endpoint de creación fija el origen de la orden en
  `ADVISOR` en el servidor; no acepta el origen del cuerpo de la petición, y una
  orden de origen aseguradora no puede crearse por esta vía (regla afirmada en
  `AGENTS.md` §2.E).

## 4. CORS y frontera del navegador (Q1)

**Decisión:** nginx hace de proxy inverso de `/api/*` hacia el gateway y el frontend
usa la ruta relativa `/api/orders`.

- El navegador habla siempre con el mismo origen (el del frontend), así que no hay
  preflight ni cabecera `Access-Control-*` en el camino normal.
- El gateway **mantiene un filtro CORS restringido** al origen
  `http://localhost:8080`, sin comodines y sin credenciales cross-origin. Es defensa
  en profundidad: si alguien llama a `http://localhost:8081` directamente desde otro
  origen, el gateway lo rechaza.
- El proxy de nginx no reenvía cabeceras `Origin` ni expone el gateway fuera de la red
  `workshop-net`; el 8081 se publica para el taller, pero no es el camino del flujo.

## 5. Cabeceras de seguridad en el frontend

nginx añade en cada respuesta del sitio estático:

| Cabecera | Valor | Motivo |
|----------|-------|--------|
| `X-Content-Type-Options` | `nosniff` | Evita que el navegador interprete otra respuesta como tipo distinto |
| `X-Frame-Options` | `DENY` | La interfaz no se embebe en marcos |
| `Referrer-Policy` | `no-referrer` | No se envía la URL del taller a terceros |
| `Content-Security-Policy` | `default-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; script-src 'self'` | Sin CDNs ni scripts de terceros |

## 6. Gestión de secretos (NFR7.1)

- Ninguna credencial real en el repositorio ni en la imagen. Las variables de entorno
  se leen de un `.env` local no versionado; `.env.example` documenta cada variable con
  un valor de ejemplo.
- Los scripts de inicialización de PostgreSQL y la configuración de RabbitMQ leen las
  credenciales de variables de entorno; los scripts no contienen valores.
- **Cambio de credenciales y volúmenes:** las credenciales de PostgreSQL y RabbitMQ
  quedan escritas en el volumen cuando el contenedor inicializa la base. Cambiar una
  contraseña exige resetear el volumen correspondiente; el procedimiento queda
  documentado junto al de respaldo (`docs/runbooks/backup-restore.md`).
- Sin credenciales en los logs: el arranque registra usuario y base, nunca la
  contraseña.

## 7. Cifrado

- **En tránsito:** no hay TLS en el entorno local; el tráfico no sale del host. El
  diseño deja el punto de entrada claro: cuando exista exposición fuera de la red
  local, TLS termina en nginx y el gateway reenvía por la red interna, y RabbitMQ y
  PostgreSQL exigirán AMQPS y `sslmode=require`.
- **En reposo:** PostgreSQL cifra los datos en disco según el sistema de archivos del
  host; no hay cifrado a nivel de columna, que se difiere junto con el WORM que
  `ARCHITECTURE.md` deja pendiente.

## 8. Auditoría e integridad del historial (NFR4.1–NFR4.4)

- La tabla `order_status_history` se crea por migración con el rol propietario, con
  `REVOKE UPDATE, DELETE, TRUNCATE ... FROM orders_user` y un trigger `BEFORE
  UPDATE OR DELETE` que lanza excepción. La cadena de hash se calcula en el servicio,
  con la serialización canónica fijada en NFR4.2.
- El rol de aplicación no puede saltarse el trigger porque no tiene permiso para
  modificar la tabla; una escritura directa con `psql` y el rol de aplicación también
  falla.
- El rol propietario sí puede modificar la tabla (es el dueño), y por eso su
  credencial solo existe para el proceso de migración, en un contenedor con acceso
  restringido a su base.

## 9. Exposición de puertos

Solo se publican 8080 (frontend), 8081 (gateway) y 15672 (management de RabbitMQ).
PostgreSQL (5432), AMQP (5672) y los puertos internos de los servicios (8082/8083)
viven dentro de `workshop-net`. La verificación es `podman compose ps` (NFR2.3).

## 10. Lo que este diseño deja para después

| Control | Por qué no entra | Cuándo |
|---------|------------------|---------|
| Autenticación OIDC/JWT y RBAC | Fuera de alcance en esta rebanada | Bloque de identidad, con Keycloak en 8180 |
| Rate limiting en el gateway | Stack local, un usuario | Ambiente compartido o exposición externa |
| TLS | Sin exposición fuera del host | Exposición externa |
| Cifrado en reposo por columna y WORM | La auditoría append-only ya cubre la detección en esta rebanada | Requisitos probatorios del cliente (`ARCHITECTURE.md`) |
| Auditoría genérica de cambios de tablas | No hay eventos de dominio más allá de la creación de órdenes | Rebanada de eventos de dominio (NFR5.8) |
