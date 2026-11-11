# Preguntas — Practices Discovery

> Cómo trabaja este equipo hoy. Cada pregunta cierra algo que la evidencia del
> repositorio no alcanza a sostener; cuando una opción se propone como la más
> probable, se dice por qué. Puedes responder con la letra de la opción o en tus
> propias palabras.

## Q1. [entrega] Integración de ramas: ¿un solo commit por PR o historial con ramas?

Hoy `main` tiene un commit y la rama de trabajo `feature/realease/1-architecture` está un commit por delante sin integrar. Esa es la única muestra, así que no puedo afirmar cómo integran ustedes: un squash deja un commit por entrega y un historial lineal; un merge commit conserva el rastro de la rama.

- A. Squash merge: cada PR deja un solo commit en `main`, con el título del PR como mensaje
- B. Merge commit: la rama queda reflejada en el historial de `main`
- C. Rebase y fast-forward: sin commit de merge, la rama se aplana sobre `main`
- D. Sin preferencia: lo que decida quien integra
- E. Aún no está definido; lo fijamos ahora
- X. Other (please specify)

[Answer]: E. Aún no está definido; lo fijamos ahora — por ahora la única rama publicada es `feature/realiease/1-architecture`, este desarrollo todavía no se puede considerar un `main`; los commits van en inglés y en orden normal

## Q2. [entrega] ¿Construir primero una rebanada delgada de punta a punta?

Una rebanada vertical mínima ("walking skeleton") es una versión mínima que corre de punta a punta —contenedor arriba, base conectada, un endpoint o consumidor respondiendo— y se construye antes de las funcionalidades reales para comprobar que las piezas encajan. No hay rastro de que la corran: el repositorio ya viene andamiado con los cuatro componentes.

- A. No correr la ceremonia: el esqueleto ya existe; cada componente nuevo arranca por su rebanada mínima
- B. Sí, siempre: antes de cada funcionalidad nueva, una rebanada verificada de punta a punta
- C. Solo cuando se agregue un componente nuevo al stack
- D. Depende del tamaño del cambio; lo decide quien implementa
- X. Other (please specify)

[Answer]: B. Sí, siempre: antes de cada funcionalidad nueva, una rebanada verificada de punta a punta — verificar partes pequeñas a medida que se implementan, para no llevarse sustos cuando se armen los bloques completos del sistema

## Q3. [calidad] El piso de 80 %: ¿líneas o ramas, y con qué herramienta?

`AGENTS.md` §5 y `DECISIONES.md` [2] piden cobertura ≥80 % con JUnit + Mockito, pero no hay JaCoCo en los `pom.xml`, ni umbral en Vitest, ni Mockito declarado. Sin herramienta y sin unidad de medida el piso no se puede verificar ni hacer cumplir en CI.

- A. 80 % de líneas, medido con JaCoCo en backend y cobertura de Vitest en frontend, y el build falla por debajo
- B. 80 % de ramas (branch coverage) con las mismas herramientas
- C. 80 % de líneas como objetivo de revisión, sin romper el build
- D. Subir el piso para los invariantes críticos (concurrencia, idempotencia, append-only) y dejar el 80 % general como meta
- X. Other (please specify)

[Answer]: A. 80 % de líneas, medido con JaCoCo en backend y cobertura de Vitest en frontend, y el build falla por debajo — la fuente de verdad es `AGENTS.md` §5; `DECISIONES.md` es del desarrollador y no se usa como fuente para el agente

## Q4. [calidad] ¿Cuándo se escriben las pruebas: antes o después del código?

Lo único que existe son pruebas de contexto (`@SpringBootTest` con `contextLoads()`) y un spec de Angular generado con el andamiaje: no hay rastro de ciclos rojo-verde ni documento que nombre TDD, BDD o ATDD. Por eso propongo "después de implementar cada capa", pero es una lectura mía.

- A. Después de implementar cada capa verificable, y la suite completa en verde antes de integrar
- B. Antes: la prueba se escribe primero y guía la implementación (TDD)
- C. Escenarios BDD primero y pruebas unitarias de detalle después
- D. Mixto: TDD para lógica de dominio, después para adaptadores e integración
- X. Other (please specify)

[Answer]: A. Después de implementar cada capa verificable, y la suite completa en verde antes de integrar — solo existen los esqueletos de front y back; de ahí en adelante se revisa qué hay y qué se debe hacer

## Q5. [operaciones] ¿Existen staging y pipeline, o todo se queda en local con Podman?

`ARCHITECTURE.md` deja AMB-02 (QA) y AMB-03 (producción) como destinos lógicos "pendientes de infraestructura definitiva", no hay pipeline en el repositorio y el despliegue documentado es local con Podman Compose. Esto define qué debe verificar la etapa de pipeline y con qué cadencia.

- A. Todo local por ahora: el despliegue es manual con `podman compose up --build` y se verifica antes de cerrar la entrega; staging y producción quedan como destino
- B. Staging real en el horizonte: despliegue automático a staging al integrar y producción con aprobación manual
- C. Solo local, pero con pipeline de CI (build y pruebas) en GitHub Actions desde ya
- D. Staging y producción completos, con pipeline y promoción entre ambientes
- X. Other (please specify)

[Answer]: A. Todo local por ahora: el despliegue es manual con `podman compose up --build` y se verifica antes de cerrar la entrega; staging y producción quedan como destino

## Q6. [desarrollo] ¿Se agregan linters ahora o se sostiene solo con Prettier?

Prettier está configurado en el frontend y `.editorconfig` fija espacios y comillas; el backend usa tabuladores de Spring Initializr. No hay ESLint, Checkstyle ni Spotless en ningún componente, así que no puedo afirmar que no los quieran: solo que hoy no están.

- A. Agregar ESLint en frontend y Spotless (o Checkstyle) en backend ahora, y el incumplimiento bloquea la integración
- B. Solo Prettier y `.editorconfig` por ahora; los linters se agregan cuando haya CI
- C. Frontend con ESLint; backend sin linter adicional
- D. Backend con Spotless; frontend solo con Prettier
- X. Other (please specify)

[Answer]: B. Solo Prettier y `.editorconfig` por ahora; los linters se agregan cuando haya CI — lo importante por ahora es que el front se conecte al back y se respete la arquitectura; ESLint no es tan necesario, pero sí seguir las buenas prácticas de desarrollo (SOLID, ACID, etc.)

## Q7. [arquitectura] ¿Un usuario de base de datos por servicio o el usuario único para el taller?

`AGENTS.md` exige una base por servicio, y `ARCHITECTURE.md` (TAC-006) pide además un usuario de base por servicio; el `compose.yaml` versionado usa un único usuario `postgres` para `orders_db` y `appointments_db`. La respuesta cambia lo que los scripts de inicialización deben crear.

- A. Un usuario y una contraseña por base (`orders_user`/`orders_db`, `appointment_user`/`appointments_db`), con permisos solo sobre su propia base
- B. Un usuario único para el entorno local del taller; los usuarios por servicio se agregan fuera de local
- C. Un usuario administrador más un usuario de aplicación por servicio
- X. Other (please specify)

[Answer]: A. Un usuario y una contraseña por base, con permisos solo sobre su propia base — se evaluó con la arquitectura y se le hace caso (TAC-006); al llegar al último alcance el usuario debe poder entrar como admin y luego, por ejemplo, como mecánico

## Q8. [producto] El `compose.yaml` actual: ¿se reescribe en su lugar o se corrige por partes?

El archivo existe y está versionado, pero no cumple: usa `POSTGRES_MULTIPLE_DATABASES` (variable que la imagen oficial ignora), monta un `./init-db.sql` que no existe, no define `workshop-net`, publica 5432 al host y declara `version: '3.8'` obsoleto. No es una decisión de prácticas, pero condiciona el trabajo siguiente.

- A. Reescribir el archivo desde cero sobre el estándar, conservando solo lo que ya sirve (imágenes y nombres de servicio)
- B. Corregir por partes sobre el archivo actual, dejando registro de cada cambio
- C. Reescribir y además documentar la propuesta anterior rechazada en `DECISIONES.md`
- X. Other (please specify)

[Answer]: A. Reescribir el archivo desde cero sobre el estándar — el `compose.yaml` actual es solo un template, se puede modificar lo que haga falta y se le hace caso a la arquitectura; la fuente de verdad de lo que se le debe al cliente es `CHALLENGE.md`

## Consolidated Summary Confirmation

- Integración: todavía no hay un `main` estable; la única rama publicada es `feature/realiease/1-architecture`, los commits van en inglés y en orden normal.
- Rebanada vertical: sí, siempre — se verifican partes pequeñas de punta a punta a medida que se implementan, antes de armar los bloques completos.
- Cobertura: piso de 80 % de líneas con JaCoCo en backend y umbral de Vitest en frontend; el build falla por debajo. La fuente de verdad es `AGENTS.md`; `DECISIONES.md` es del desarrollador y no se usa como fuente.
- Cadencia de pruebas: después de implementar cada capa verificable, con la suite completa en verde antes de integrar.
- Entornos: todo local con Podman y `podman compose up --build`; staging y producción quedan como destino.
- Linters: solo Prettier y `.editorconfig` por ahora; lo prioritario es conectar front con back respetando la arquitectura y las buenas prácticas (SOLID, ACID).
- Base de datos: un usuario y una contraseña por base, con permisos solo sobre su propia base, según la arquitectura; más adelante el acceso será por rol (admin, mecánico, etc.).
- `compose.yaml`: se reescribe desde cero sobre el estándar y la arquitectura; el entregable al cliente se rige por `CHALLENGE.md`.

- Looks correct
- Request changes

[Answer]: Looks correct
