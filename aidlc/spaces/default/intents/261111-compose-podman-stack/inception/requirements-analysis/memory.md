<!-- INVARIANT: examples are single-line HTML comments so a fresh template parses to total=0 (MEMORY_EMPTY). Do NOT un-comment or split across lines. t100 guards this. -->
> This file is kept up to date automatically while the stage runs. Add observations at the review step, not by editing here directly.

## Interpretations
- 2026-11-11T13:38:00Z — "un mini flujo" se interpretó como crear la orden, listarla y abrir su detalle con el historial inicial; se dejó explícito en S3 y fuera de alcance el resto de transiciones. La persona confirmó el resumen consolidado con `Looks correct`.
- 2026-11-11T13:38:00Z — el pedido de "usuarios de prueba" (Q4) se resolvió como usuarios de aplicación para el bloque de autenticación, no como roles de base; los scripts de esta rebanada solo crean roles de PostgreSQL (F3).

## Deviations
- 2026-11-11T13:38:00Z — el entregable declarado al inicio era `compose.yaml` más scripts SQL; las respuestas Q1 y F4 lo amplían a los `Dockerfile` de los cuatro componentes, un gateway mínimo y la rebanada de órdenes. Se dejó registrado como requisito, no como supuesto oculto.

## Tradeoffs
- 2026-11-11T13:38:00Z — se aceptó un único contenedor PostgreSQL con dos bases y dos roles sin acceso cruzado, en lugar de dos contenedores, porque `ARCHITECTURE.md` lo contempla para desarrollo y la persona priorizó un stack liviano (Q2, F1).
- 2026-11-11T13:38:00Z — no se siembran usuarios de aplicación en esta rebanada: sembrarlos sin autenticación habría creado credenciales sin dueño y habría adelantado el bloque de identidad (F3).

## Open questions
- 2026-11-11T13:38:00Z — falta el catálogo definitivo de estados y el nombre oficial del estado inicial; se usó `RECIBIDA` del catálogo propuesto y queda como O1/O2 para la validación funcional.
- 2026-11-11T13:38:00Z — el rol que reabre una orden (`SUPERVISOR`) necesita equivalencia con usuarios reales; se define en el bloque de autenticación (O4).
