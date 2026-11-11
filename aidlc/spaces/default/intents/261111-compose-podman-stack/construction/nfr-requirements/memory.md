<!-- INVARIANT: examples are single-line HTML comments so a fresh template parses to total=0 (MEMORY_EMPTY). Do NOT un-comment or split across lines. t100 guards this. -->
> This file is kept up to date automatically while the stage runs. Add observations at the review step, not by editing here directly.

## Interpretations
- 2026-11-11T14:05:00Z — "lo más indispensable" en seguridad (Q3) se leyó como la opción A sin rate limiting: validación básica, CORS restringido, secretos por variables y sin puertos de datos; CORS no se puede diferir porque el frontend (8080) y el gateway (8081) son orígenes distintos.
- 2026-11-11T14:05:00Z — "sin escalar ni preparar" (Q5) se registró como ausencia de escalado horizontal explícito, sin exigir trabajo previo de preparación que nadie va a usar.

## Deviations
- 2026-11-11T14:05:00Z — el objetivo de rendimiento de `requirements.md` NFR8 (1.000 órdenes en menos de 1 s) se diferenció a la rebanada de lógica de negocio y se sustituyó por una línea base de humo de 2 s; la diferencia queda anotada como NFR8.2 y en el apartado Diferido del artefacto de rendimiento.
- 2026-11-11T14:05:00Z — los requisitos de escalabilidad no tienen un NFR de inception propio; se expresan como NFR8.5–NFR8.8 y la decisión queda registrada como ADR en `tech-stack-decisions.md`.

## Tradeoffs
- 2026-11-11T14:05:00Z — se fijó el formato del hash (SHA-256 con serialización canónica y génesis de 64 ceros) en esta etapa, y no en la implementación, porque `order_status_history` es append-only y corregir el formato después obligaría a rehacer el historial.
- 2026-11-11T14:05:00Z — sin SLO, sin métricas y sin trazas: la observabilidad mínima (logs JSON con `correlationId`, Actuator health y management del broker) cubre el diagnóstico local sin gastar presupuesto de operación.

## Open questions
- 2026-11-11T14:05:00Z — la medición de NFR1.1 (10 minutos) depende de la máquina del taller y de la caché de build; hay que registrar ese número en la entrega.
- 2026-11-11T14:05:00Z — falta definir en qué carpeta se documenta el procedimiento de `pg_dump` (NFR1.5) para que quede referenciable desde la entrega.
- 2026-11-11T14:25:00Z — la revisión adversarial (iteración 1, 13 hallazgos) drove a cerrar el formato del hash con prefijo de longitud, a differentiating el diferido de autenticación del requisito vigente del gateway, a reformular los criterios de cadena de hash para lo que esta rebanada produce, y a anclar la medición de rendimiento a un host con límites de recursos.
