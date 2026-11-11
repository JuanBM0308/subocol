# Requisitos de fiabilidad — Stack Podman y rebanada de órdenes

> Etapa NFR Requirements. Deriva de `requirements.md` (NFR1, NFR4) y de las
> respuestas confirmadas en `nfr-requirements-questions.md` (Q2 y Q6). Contexto de
> referencia: `requirements-analysis/requirements.md`, `AGENTS.md` §3,
> `ARCHITECTURE.md` (AMB-01, PAT-004).
> El taller es local y de un día: la fiabilidad se mide como "arranca y se
> sostiene", no como disponibilidad medida en una ventana temporal.

## Alcance

Cubre el arranque del stack, la recuperación ante fallos y la durabilidad de los
datos que el taller necesita recuperar a mano.

## Requisitos

| ID | Requisito | Criterio de verificación |
|----|-----------|--------------------------|
| **NFR1.1** | Desde un clon limpio, `podman compose up --build` deja todos los contenedores en estado `healthy` en 10 minutos o menos. | Medición del comando en la máquina del taller con caché de build vacía; el tiempo se registra en la entrega. |
| **NFR1.2** | El orden de arranque está garantizado por dependencias: cada servicio espera a que sus dependencias estén `healthy` mediante `depends_on` con `condition: service_healthy`. | El compose declara la condición; en el arranque el servicio no acepta tráfico antes de que su base acepte conexiones. |
| **NFR1.3** | Un contenedor cuyo proceso muere se reinicia solo (`restart: unless-stopped`) y su reinicio queda visible en el contador de reinicios y en el estado de `podman compose ps`. Un contenedor que no logra arrancar (imagen o configuración inválida) se detecta por su estado de reinicio o de salida, no por `unhealthy`. | La caída de un proceso produce reinicio automático visible en `podman compose ps`; un contenedor que no arranca aparece reiniciando o saliente. El estado `unhealthy` queda reservado para el caso de proceso vivo con dependencia caída, que es el de NFR5.4. |
| **NFR1.4** | Los datos sobreviven al reinicio de los contenedores: PostgreSQL y RabbitMQ usan volúmenes nombrados y `podman compose down` no los borra. | Tras `podman compose down` y un nuevo `up`, la orden registrada sigue existiendo. |
| **NFR1.5** | Los datos se pueden respaldar y restaurar a mano con `pg_dump` y `psql`, con el procedimiento documentado en `docs/runbooks/backup-restore.md` (creado en la etapa de construcción). | El procedimiento de `docs/runbooks/backup-restore.md` se ejecuta de punta a punta y devuelve una copia de las órdenes y su historial. |
| **NFR4.5** | La cadena de hash del historial se conserva entre respaldos: restaurar un respaldo mantiene la verificabilidad de la cadena. | Tras restaurar, la verificación de cadena recorre todas las filas sin romperse. |
| **NFR4.6** | El historial de una orden en esta rebanada contiene su fila inicial, y la génesis y el hash de esa fila son verificables de forma independiente. | La verificación de génesis y hash se ejecuta sobre el historial de una orden recién creada y coincide; el recorrido de una cadena con varias filas por orden llega con la rebanada del ciclo de estados (ver «Diferido»). |

## Notas de diseño

- **Sin SLO formal, con criterio explícito.** No se declara un SLO de disponibilidad
  ni una ventana de medición: no hay dónde medirlo y el número sería ficticio. El
  criterio de esta fase es que el stack arranque saludable y se sostenga durante la
  jornada de trabajo.
- **El fallo se recupera sin intervención.** La combinación de `healthcheck`,
  `depends_on` y `unless-stopped` cubre los dos fallos típicos de un entorno local:
  un servicio que arranca antes que su base y un proceso que se cae.
- **El respaldo es manual a propósito.** Un taller con un día de entrega no
  justifica un servicio de respaldos; lo que no puede faltar es el procedimiento
  para exportar y restaurar, porque sin él, perder el volumen es perder el trabajo
  de la sesión.
- **La cadena sobrevive al respaldo.** Como el hash encadenado es lo que da
  detectabilidad ante alteraciones (PAT-004), restaurar un respaldo tiene que dejar
  el historial verificable; por eso el requisito es de la cadena y no solo del
  volumen.

## Diferido

| ID | Motivo | Cuándo se retoma |
|----|--------|------------------|
| SLO de disponibilidad (NFR1, ventanas de medición) | No hay un ambiente estable ni una ventana de observación en un entorno local de taller. | Cuando exista staging con despliegue al integrar. |
| Respaldo automático periódico | No hay dónde respaldar (ni volumen, ni destino externo) y el costo de operación no se justifica en esta fase. | Junto con la definición de infraestructura definitiva (`AMB-02`, `AMB-03`). |
| Verificación de la cadena con varias filas por orden (NFR4.3) y recorrido completo del historial (NFR4.6) | En esta rebanada cada orden tiene una sola fila de historial, porque las transiciones de estado están fuera de alcance; no hay segunda fila que encadenar. | Rebanada del ciclo de estados, que produce la segunda fila por orden y hace verificable el encadenamiento completo. | No hay dónde respaldar (ni volumen, ni destino externo) y el costo de operação no se justifica en esta fase. | Junto con la definición de infraestructura definitiva (`AMB-02`, `AMB-03`). |
