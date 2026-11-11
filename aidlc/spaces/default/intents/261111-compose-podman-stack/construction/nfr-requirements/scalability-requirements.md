# Requisitos de escalabilidad — Stack Podman y rebanada de órdenes

> Etapa NFR Requirements. Deriva de `requirements.md` (NFR8) y de las respuestas
> confirmadas en `nfr-requirements-questions.md` (Q1 y Q5). Contexto de referencia:
> `requirements-analysis/requirements.md`, `AGENTS.md` §3, `ARCHITECTURE.md`
> (PAT-001, TAC-005).
> `requirements.md` no define un requisito no funcional dedicado a escalabilidad;
> las metas de escalabilidad de esta rebanada se expresan como restricciones de
> capacidad bajo NFR8, y la decisión de no escalar queda registrada en
> `tech-stack-decisions.md`.

## Alcance

Describe la capacidad que el stack soporta en la fase actual y deja explícito qué
no se prepara todavía. La decisión de la persona fue clara: escalar no es relevante
ahora; el esfuerzo se pone en la lógica de negocio y en el front.

## Requisitos

| ID | Requisito | Criterio de verificación |
|----|-----------|--------------------------|
| **NFR8.5** | Una sola instancia por servicio: el `compose.yaml` no define réplicas, `deploy.replicas` ni estrategias de balanceo. | El compose no contiene claves de escalado; `podman compose ps` muestra una instancia por servicio. |
| **NFR8.6** | No se implementa preparación específica de escalado en esta fase (ni sesiones compartidas, ni particionado, ni réplicas de lectura). | No hay código ni configuración de escalado en el stack; la decisión queda documentada como revisable. |
| **NFR8.7** | La capacidad de la fase se documenta como el mínimo verificable: 1 usuario concurrente y 1 orden almacenada en la prueba de humo; el crecimiento real se dimensiona cuando exista carga del taller. | La documentación de la rebanada declara este límite y no promete capacidad que no se ha medido. |
| **NFR8.8** | El aislamiento entre dominios se mantiene aunque el stack crezca: cada servicio sigue hablando solo con su base y con su broker por API o evento, nunca con la base de otro. | Revisión de configuración y de código: ninguna consulta cruzada entre `orders_db` y `appointments_db`. |

## Notas de diseño

- **La frontera que sí se respeta.** El reparto por base de datos (una base por
  dominio, un rol por base) es la decisión que permite escalar cada servicio por
  separado más adelante sin rediseñar nada. Es la razón por la que el aislamiento
  se documenta como requisito (NFR8.8) y no como detalle de implementación.
- **Qué pasa si el taller crece.** Con una instancia por servicio y sin particionado,
  el límite real es el del contenedor PostgreSQL. Cuando aparezca carga real, el
  orden de intervención natural es: réplicas del gateway y de `order-service`
  (son sin estado), pool de conexiones por servicio, y lectura por réplica para el
  listado. Nada de eso se implementa ahora.
- **Por qué no se prepara nada.** Spring Boot, PostgreSQL y RabbitMQ no añaden
  complejidad por ser sin estado o por admitir réplicas; lo que sí costaría es
  escribir y mantener configuración de escalado que nadie va a usar. El costo real
  está en la operación local, que es el recurso escaso del taller.

## Diferido

| ID | Motivo | Cuándo se retoma |
|----|--------|------------------|
| NFR8.6 y NFR8.7 | No hay carga real ni equipo definido; comprometer capacidad hoy sería inventar el número. | Rebanada de lógica de negocio, con medición sobre el uso real del taller. |
