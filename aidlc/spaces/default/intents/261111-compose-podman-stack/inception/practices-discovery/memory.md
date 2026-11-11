<!-- INVARIANT: examples are single-line HTML comments so a fresh template parses to total=0 (MEMORY_EMPTY). Do NOT un-comment or split across lines. t100 guards this. -->
> This file is kept up to date automatically while the stage runs. Add observations at the review step, not by editing here directly.

## Interpretations
<!-- example: 2026-05-29T10:14:32Z — chose REST over GraphQL; the consuming team only needs CRUD, revisit if subscriptions land -->
- 2026-11-11T05:05:49Z — el `compose.yaml` de la raíz se leyó como evidencia y no como práctica afirmada; ya está versionado pero incumple las restricciones del taller, así que su diagnóstico quedó en `evidence.md` y no se convirtió en regla del equipo.
- 2026-11-11T05:05:49Z — `team-practices.md` se escribió como práctica durable del espacio, no específica de esta entrega; el detalle del stack de compose se resolvió en `discovered-rules.md` y en el diagnóstico de evidencia.

## Deviations
<!-- example: 2026-05-29T10:14:32Z — skipped the optional caching layer the stage prose suggested; the dataset is small enough that it adds risk -->
- 2026-11-11T05:05:49Z — no se abrió revisión de spokes; `support_agents` llegó vacío (Collaborators: off) y el paso 3 se omitió entero, según `stage-protocol-ensemble.md` §5. El borrador del lead se sostiene solo y la entrevista resuelve las prácticas.
- 2026-11-11T05:05:49Z — no se inspeccionaron los artefactos upstream de reverse-engineering porque reverse-engineering está en SKIP en este plan; la inspección directa del workspace los sustituyó y así se declaró en `evidence.md`.

## Tradeoffs
<!-- example: 2026-05-29T10:14:32Z — picked TDD over BDD this run; the team is unit-first and the domain is well-understood -->
- 2026-11-11T05:05:49Z — se eligió `test-after` sobre `tdd`/`bdd`; la única evidencia son pruebas de contexto y un spec de Angular escritos con el andamiaje, sin ciclos rojo-verde ni documentos que nombren otra cadencia.
- 2026-11-11T05:05:49Z — el piso de 80 % se registró como regla dura en `discovered-rules.md` en lugar de diluirlo en la postura de pruebas; está escrito por una persona en dos archivos y condiciona el pipeline de CI que viene.
- 2026-11-11T05:05:49Z — se propuso squash merge por coherencia con el default de la organización y porque `main` tiene un solo commit, aun sin evidencia directa de un PR integrado; queda como pregunta de entrevista.
- 2026-11-11T05:05:49Z — se mantuvo la fuerza de cada inferencia explícita (Fuerte/Media/Débil) en `evidence.md` en lugar de presentar todo como afirmado; el equipo decide qué sostener.

## Open questions
<!-- example: 2026-05-29T10:14:32Z — confirm the retention window with compliance before the next stage hardens the schema -->
- 2026-11-11T05:05:49Z — la rama actual se llama `feature/realease/1-architecture` con un error de tipeo; conviene confirmar si la convención de nombres se corrige ahora o se deja como deuda.
- 2026-11-11T05:05:49Z — `ARCHITECTURE.md` TAC-006 pide usuarios de base por servicio mientras el compose existente usa un único usuario `postgres`; hay que decidir si el estándar incluye credenciales separadas.
- 2026-11-11T05:05:49Z — no hay `Dockerfile` en ningún componente y los `build:` del compose apuntan a directorios sin `pom.xml`; confirmar dónde se resuelve eso antes de intentar un `podman compose up --build` real.
