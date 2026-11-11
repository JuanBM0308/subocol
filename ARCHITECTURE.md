**TALLER 360 SUBOCOL | DISEÑO DE ARQUITECTURA**

# DOCUMENTO DE DISEÑO DE ARQUITECTURA

## Taller 360 Subocol

#### Sistema de órdenes de servicio, agenda y trazabilidad para taller automotriz

##### Prueba técnica-Desarrollador Full Stack / Subocol

##### 1.0 - Propuesta de arquitectura para prueba técnica

##### 11 de noviembre de 2026

##### Estado: Propuesta técnica sujeta a validación funcional

*Documento elaborado con base en la guía de arquitectura suministrada y el caso de estudio.*

Propuesta para prueba técnica Subocol • Página 1

#### 1.1 Historial de versiones

*Tabla 1. Historial de versiones*

|Versión|Fecha|Descripción|Responsable|
|---|---|---|---|
|||Versión inicial de propuesta arquitectónica; incluye vistas, decisiones, restricciones y dia- gramas editables.||
|1.0|11-11-2026||Juan Sebastian Barbosa Mora (jsbm259@gmail.com)|

*Tabla 2. Aprobación y validación*

|Instancia|Responsable|Estado|
|---|---|---|
|Patrocinador / responsable del taller|Por definir|Pendiente|
|Responsable funcional|Por definir|Pendiente|
|Responsable técnico|Por definir|Pendiente|
|Evaluador de la prueba|Subocol|Para revisión|

#### 1.2 Tabla de contenido

1.1 Historial de versiones................................................................................................................................................2
1.2 Tabla de contenido....................................................................................................................................................2
1.3 Índice de ilustraciones...............................................................................................................................................3
1.4 Índice de tablas.........................................................................................................................................................3
1. Documento de arquitectura...............................................................................................................................................4
1.5 Introducción...............................................................................................................................................................4
1.6 Objeto........................................................................................................................................................................4
1.7 Alcance......................................................................................................................................................................4
Incluido en la propuesta.............................................................................................................................................4

Fuera del alcance del MVP........................................................................................................................................4

1.8 Definiciones...............................................................................................................................................................5
1.9 Interesados del diseño de arquitectura.....................................................................................................................5
1.10 Requerimientos funcionales de alto nivel-RF........................................................................................................6
1.11 Atributos de Calidad................................................................................................................................................7
1.12 Restricciones...........................................................................................................................................................8
1.13 Decisiones arquitectónicas de diseño.....................................................................................................................9
1.13.1 Tácticas...........................................................................................................................................................9
1.13.2 Patrones........................................................................................................................................................10
1.13.3 Frameworks...................................................................................................................................................10
1.14 Vistas de la arquitectura........................................................................................................................................11
1.14.1 Vista lógica....................................................................................................................................................11
Modelo lógico relacional...........................................................................................................................................12

1.14.2 Vista de desarrollo.........................................................................................................................................13
1.15 Vista de procesos..................................................................................................................................................13
1.15.1 Vista física.....................................................................................................................................................18
1.15.2 Vista de escenarios.......................................................................................................................................18
1.16 Ambientes implementados....................................................................................................................................19
1.17 Dimensionamiento-crecimiento...........................................................................................................................20

1.18 Componentes habilitadores del desarrollo............................................................................................................20
1.19 Riesgos de la arquitectura.....................................................................................................................................21
2. Conclusiones..................................................................................................................................................................22
#### 1.3 Índice de ilustraciones

Ilustración 1. Modelo de clases del dominio.......................................................................................................................12
Ilustración 2. Modelo lógico relacional por servicio.............................................................................................................13
Ilustración 3. Diagrama de componentes...........................................................................................................................13
Ilustración 4. Secuencia de registro/cambio de estado y auditoría.....................................................................................14
Ilustración 5. Secuencia de agendamiento y notificación...................................................................................................14
Ilustración 6. Secuencia de cierre y cancelación silenciosa...............................................................................................15
Ilustración 7. Secuencia de reapertura con motivo obligatorio...........................................................................................15
Ilustración 8. Secuencia de cancelación manual y notificación..........................................................................................16
Ilustración 9. Actividad de cierre y cancelación automática................................................................................................18
Ilustración 10. Diagrama de despliegue local con Podman................................................................................................18
Ilustración 11. Diagrama de casos de uso..........................................................................................................................19

#### 1.4 Índice de tablas

Tabla 1. Historial de versiones..............................................................................................................................................2
Tabla 2. Aprobación y validación..........................................................................................................................................2
Tabla 3. Definiciones y acrónimos........................................................................................................................................6
Tabla 4. Interesados del sistema..........................................................................................................................................6
Tabla 5. Requerimientos funcionales de alto nivel...............................................................................................................7
Tabla 6. Atributos de calidad................................................................................................................................................8
Tabla 7. Restricciones de arquitectura.................................................................................................................................9
Tabla 8. Tácticas de arquitectura........................................................................................................................................10
Tabla 9. Patrones de arquitectura y diseño........................................................................................................................11
Tabla 10. Frameworks y componentes habilitadores.........................................................................................................11
Tabla 11. Ambientes del sistema........................................................................................................................................20
Tabla 12. Estimación y variables de crecimiento................................................................................................................21
Tabla 13. Habilitadores de desarrollo.................................................................................................................................21
Tabla 14. Riesgos de arquitectura......................................................................................................................................22

### 1. Documento de arquitectura

#### 1.5 Introducción

El taller automotriz objeto de la prueba recibe vehículos de clientes particulares y de aseguradoras. La operación actual se apoya en hojas de cálculo y conversaciones de WhatsApp, lo cual dificulta mantener un flujo controlado de órdenes, administrar la capacidad de los asesores, automatizar cancelaciones relacionadas con el cierre de una orden y demostrar quién modificó los estados del proceso.

La solución propuesta, denominada provisionalmente Taller 360 Subocol, organiza el dominio en dos microservicios de negocio: Order Service y Appointment Service. Un worker de notificaciones consume eventos para simular el canal WhatsApp mediante logs. Angular constituye la interfaz web; Java con Spring Boot implementa los servicios; RabbitMQ desacopla los eventos de dominio; PostgreSQL persiste los datos por servicio; y Podman ejecuta localmente los componentes mediante una definición Compose compatible.

La prueba dispone de un horizonte de un día. Por ello, la arquitectura prioriza los invariantes de negocio y los puntos de mayor riesgo: auditoría transaccional, capacidad concurrente de agenda, cancelación silenciosa al cerrar órdenes e idempotencia de eventos. Las integraciones reales con aseguradoras y WhatsApp quedan preparadas como exten- siones, pero fuera del MVP.

#### 1.6 Objeto

Definir una arquitectura de solución que sirva como guía/bitácora para construir, probar, desplegar y evolucionar Taller 360 Subocol. El documento identifica interesados, requisitos de alto nivel, atributos de calidad, restricciones, decisiones, componentes, datos, interacciones, infraestructura, ambientes, riesgos y trazabilidad entre necesidades y tácticas.

#### 1.7 Alcance

##### Incluido en la propuesta

· Crear órdenes de servicio manuales por asesores para clientes y vehículos; asociar una aseguradora cuando aplique, respetando el origen permitido. · Gestionar el ciclo de vida de la orden mediante transiciones controladas; permitir reapertura únicamente con una razón obligatoria. · Mantener historial de cambios de estado con usuario y fecha/hora, con mecanismos de solo inserción y detección de alteraciones. · Agendar citas de entrega/recepción por asesor y franja horaria, verificando capacidad de manera segura ante concurrencia. · Notificar mediante un log simulado todas las citas agendadas y las cancelaciones manuales. · Al cerrar una orden, cancelar automáticamente las citas pendientes asociadas y suprimir cualquier notificación de WhatsApp para esa cancelación automática. · Reservar un contrato de integración para órdenes provenientes de aseguradoras, sin permitir que el asesor las cree manualmente. · Describir despliegue local con Podman y Compose compatible.

##### Fuera del alcance del MVP

· Integración productiva con WhatsApp Business API y conectores reales de aseguradoras. · Facturación, recaudo, inventario, compras, repuestos, nómina, contabilidad y gestión de garantías. · Gestión de fotografías/documentos, aplicación móvil, multi-sede, multi-tenant y analítica avanzada. · Plataforma WORM externa para auditoría, alta disponibilidad multinodo y recuperación ante desastres con RPO/RTO contractuales. La guía de arquitectura exige ambientes de Desarrollo, Pruebas y Producción. Se definen conceptualmente en este documento; la infraestructura productiva no está especificada por el enunciado y por tanto no se asume una topología definitiva.

#### 1.8 Definiciones

*Tabla 3. Definiciones y acrónimos*

|Término|Definición|
|---|---|
|API Gateway|Componente de entrada que enruta solicitudes y aplica controles técnicos comunes.|
|ATC|Atributo de calidad.|
|DLQ|Dead Letter Queue; cola para mensajes que agotaron los reintentos.|
|IdP / OIDC|Proveedor de identidad / OpenID Connect.|
|MVP|Producto mínimo viable para demostrar los flujos prioritarios.|
|Outbox|Tabla persistida en la misma transacción del cambio de negocio y publicada posteriormente al broker.|
|RBAC|Control de acceso basado en roles.|
|RF / RNF|Requerimiento funcional / requerimiento no funcional (Atributos de calidad).|
|WORM|Write Once, Read Many; almacenamiento con retención que impide modificar/borrar objetos durante el periodo definido.|
|Podman Compose|Proveedor de orquestación local que interpreta una especificación Compose para Podman.|
|Orden|Unidad de trabajo asociada a un cliente, vehículo, actividades y ci- clo de vida.|
|Franja / slot|Intervalo de agenda de un asesor con capacidad máxima config- urable.|

#### 1.9 Interesados del diseño de arquitectura

El enunciado no suministra nombres, cargos individuales ni contactos. Se registran roles para no inventar identidades; deberán reemplazarse/confirmarse durante el levantamiento. *Tabla 4. Interesados del sistema*

|ID|Rol|Nombre completo|Cargo|Contacto|Funciones|
|---|---|---|---|---|---|
||||||Priorizar alcance, vali- dar reglas y aprobar criterios de aceptación.|
|INT-01|Patrocinador / dueño del proceso|Por definir|Responsable del taller|Por definir|Registrar órdenes, mover estados y ges- tionar citas.|
|INT-02|Asesor de servicio|Por definir|Asesor|Por definir|Autorizar reaperturas y supervisar el flujo de trabajo.|
|INT-03|Supervisor|Por definir|Supervisor operativo|Por definir|Consultar el historial de estados y verificar traz- abilidad.|
|INT-04|Auditor|Por definir|Auditor interno|Por definir||
|INT-05|Cliente|No aplica (actor ex- terno)|Cliente particular|Dato gestionado por el taller|Recibir notificaciones y asistir a citas. Implementar servicios, seguridad, despliegue y observabilidad.|
|INT-06|Responsable técnico|Por definir|Líder técnico / DevOps|Por definir|Definir contratos con aseguradoras en una fase posterior.|
|INT-07|Responsable de inte- gración|Por definir|Líder de integración|Por definir|Revisar alineación con el caso, decisiones y entregables.|
|INT-08|Evaluador|Subocol|Evaluador de prueba técnica|Canal de la prueba||

#### 1.10 Requerimientos funcionales de alto nivel-RF

Se identifican los requerimientos de alto nivel a partir del caso de estudio. La prueba no adjunta un formato externo de requisitos, por lo que los códigos RF se proponen en este documento; el equipo funcional debe ratificar nombres, reglas y aceptación. *Tabla 5. Requerimientos funcionales de alto nivel*

|ID|Nombre del requerim- iento|Entrada principal|Proceso de alto nivel|Salida|
|---|---|---|---|---|
|||Cliente, vehículo, asegu- radora opcional, descripción de trabajo y origen.|Validar datos, permisos y ori- gen; crear orden en estado inicial y sus ítems.||
|RF-001|Registrar orden de servicio||Validar transición permitida y registrar cambio e historial en una transacción.|orderId, estado inicial y de- talle de la orden.|
|RF-002|Gestionar estados de orden|orderId, nuevo estado, iden- tidad autenticada.|Validar que la orden esté cerrada y que el motivo no esté vacío; registrar reaper- tura.|Estado actualizado e histórico del cambio.|
|RF-003|Reabrir orden cerrada|orderId, motivo obligatorio, identidad autorizada.|Validar que la orden sea elegible y solicitar reserva del cupo.|Orden reabierta y evento de auditoría.|
|RF-004|Agendar cita|orderId, asesor, franja, tipo de cita y contacto del cliente.|Bloquear slot, validar capaci- dad disponible e incrementar reserva de forma atómica.|appointmentId y confirma- ción de franja.|
|RF-005|Controlar cupos por as- esor/franja|advisorId, slotId y capacidad configurada.|Consumir evento de forma idempotente y emitir log de WhatsApp simulado para la agenda y la cancelación manual.|Cita aceptada o rechazo por falta de cupo.|
|RF-006|Notificar agenda/cancelación manual|Evento de cita y política de notificación.|Buscar citas pendientes y cancelarlas con motivo OR- DER_CLOSED y notifyCus- tomer=false.|Registro en log estructurado de notificación.|
|RF-007|Cancelar citas pendientes al cerrar orden|Evento OrderClosed con or- derId.|Consultar historial en orden cronológico, restringido por rol.|Citas canceladas sin notifi- cación al cliente.|
|RF-008|Consultar auditoría de esta- dos|orderId y filtros/paginación opcionales. Payload externo, insur- erCode, externalReference y credencial técnica.|Validar contrato y autenti- cación de integración; dedu- plicar; crear orden con ori- gen de aseguradora.|Actor, instante, estado ante- rior/nuevo, motivo y huella.|
|RF-009|Recibir órdenes de asegu- radoras (futuro)|Filtros por orden, vehículo, estado, asesor y rango de fechas.||Orden registrada una sola vez o error de validación.|
|RF-010|Consultar órdenes y agenda||Consultar mediante API del servicio propietario.|Listados paginados con datos permitidos por rol.|

Nota sobre RF-009: el caso lo ubica en una fase posterior. En el MVP se implementa como frontera/contrato documen- tado y política de rechazo para que no se pueda simular su creación por un asesor.

Como el enunciado no entrega un catálogo formal de estados, se propone para validación el flujo RECIBIDA → EN_DI-AGNOSTICO → ESPERA_APROBACION → EN_REPARACION → LISTA_PARA_ENTREGA → CERRADA. La reapertura solo parte de CERRADA, exige motivo no vacío y registra REABIERTA; desde allí continúa por la transición funcional aprobada. Son estados ilustrativos, no reglas confirmadas del taller. Para citas se propone PENDING, CAN-CELLED y COMPLETED; la cancelación manual de una cita PENDING notifica, mientras que la cancelación automática por cierre no.

#### 1.11 Atributos de Calidad

La guía presenta como obligatorios para todo sistema los atributos de desempeño, disponibilidad, seguridad, trazabilidad, usabilidad y mantenibilidad. Se agregan interoperabilidad e integridad de datos por las integraciones futuras y las reglas de cupos. El caso no proporciona códigos RNF; los valores medibles siguientes son objetivos iniciales de diseño que requieren validación y no deben leerse como SLA contractual. *Tabla 6. Atributos de calidad*

|ID|Atributo|ID-RNF|Propósito / escenario verificable|Importancia|
|---|---|---|---|---|
||||Objetivo inicial: percentil 95 de operaciones REST de lectura/escritura inferior a||
|ATC-001|Desempeño|Pendiente de formalizar||Alto|

|ID|Atributo|ID-RNF|Propósito / escenario verificable|Importancia|
|---|---|---|---|---|
||||500 ms bajo la carga de prueba acordada, ex- cluyendo latencia de provee- dor externo. Verificar con pruebas de carga y métricas. Objetivo inicial para piloto: 99,5 % mensual del acceso web/API. Los eventos se reintentan ante indisponibili- dad temporal de broker o consumidor; validar moni- toreo y recuperación.||
|ATC-002|Disponibilidad|Pendiente de formalizar|Exigir identidad OIDC/JWT, autorización por rol, TLS fuera de localhost, validación de entradas, secretos fuera del código y usuarios DB por servicio. Verificar pruebas negativas y revisión de per- misos.|Alto|
|ATC-003|Seguridad|Pendiente de formalizar|El 100 % de transiciones de estado debe producir actor, UTC timestamp, estado an- terior/nuevo y motivo cuando aplique. La escritura del es- tado e historial debe ser atómica; la tabla es append- only.|Alto|
|ATC-004|Trazabilidad|Pendiente de formalizar|Permitir que el asesor identi- fique con claridad estado ac- tual, cupos y próximas citas; validar mediante pruebas de aceptación con asesores y tareas representativas.|Alto|
|ATC-005|Usabilidad|Pendiente de formalizar|Separación por dominio y capas, contratos OpenAPI, migraciones versionadas, pruebas automatizadas y logs correlacionados. Veri- ficar facilidad de cambio y cobertura de flujos críticos.|Alto|
|ATC-006|Mantenibilidad|Pendiente de formalizar|La futura integración de ase- guradoras utilizará contrato versionado, identidad téc- nica, externalReference e idempotencia. No habilitar escrituras directas sobre DB.|Alto|
|ATC-007|Interoperabilidad|Pendiente de formalizar|No permitir sobrecupo por carrera concurrente; no perder cierre-evento por fallo entre commit y publicación; re-procesamiento idempo- tente. Verificar con pruebas concurrentes y fallos inyecta- dos.|Medio|
|ATC-008|Integridad / consistencia|Pendiente de formalizar||Alto|

#### 1.12 Restricciones

*Tabla 7. Restricciones de arquitectura*

|ID|Tipo|Descripción|
|---|---|---|
|RST-01|Software|Frontend obligatorio en Angular.|
|RST-02|Software|Backend obligatorio en Java y bajo estilo de mi- croservicios. El enunciado solicita que los componentes levan- ten con Docker Compose; el entorno de ejecución (PC Juan Barbosa) no es compatible con Docker según lo informado. Se sustituye por Podman y un compose.yaml compatible, ejecutado mediante podman compose o podman-compose.|
|RST-03|Infraestructura / Técnica||
|RST-04|Infraestructura|La disponibilidad de virtualización, Podman Ma- chine y provider Compose depende del sistema|

|ID|Tipo|Descripción|
|---|---|---|
|||operativo y hardware; debe validarse en el equipo real.|
|RST-05|Integración|WhatsApp se puede simular mediante un log; no se requiere proveedor real para demostrar el flujo. Las órdenes de aseguradoras grandes llegarán posteriormente mediante integración y no las debe crear el asesor.|
|RST-06|Negocio|La prueba indica un horizonte de un día; se prior- izan invariantes de negocio y se difieren integra- ciones externas y capacidades no solicitadas.|
|RST-07|Tiempo|Se tratarán datos personales y de vehículos. El ac- ceso, retención, enmascaramiento y tratamiento deben ajustarse a las políticas aplicables del taller y al marco legal que valide el responsable.|
|RST-08|Datos / Seguridad|El catálogo exacto de estados, transiciones, gran- ularidad de franjas, duración de citas y cupos máx- imos no está definido en el enunciado; se docu- mentan propuestas sujetas a aprobación.|
|RST-09|Negocio / Datos|Los microservicios no acceden directamente a bases de datos de otros servicios. La comuni- cación es REST o eventos de dominio.|
|RST-10|Técnica||

#### 1.13 Decisiones arquitectónicas de diseño

##### 7.13.1 Tácticas

Las tácticas implementan respuestas concretas a RF y atributos ATC. La matriz siguiente cubre todos los requerimientos funcionales y todos los atributos de calidad identificados; ningún RF o ATC queda sin táctica. *Tabla 8. Tácticas de arquitectura*

|ID|ID relacionado|Táctica asociada|
|---|---|---|
|||Validar origen de la orden desde el principal auten- ticado. Los asesores solo crean órdenes de origen ADVISOR; el futuro adaptador utiliza un principal técnico con alcance específico. Para aseguradoras usar externalReference único e idempotencia.|
|TAC-001|RF-001, RF-009, ATC-003, ATC-007|Implementar una máquina de estados explícita en Order Service. Rechazar transiciones no permiti- das; exigir motivo no vacío al reabrir una orden cerrada. Registrar la reapertura como evento/es- tado distinto.|
|TAC-002|RF-002, RF-003|Actualizar el estado, insertar el historial append- only y persistir Outbox en la misma transacción. Actor desde JWT; UTC timestamp; hash encade- nado; permisos SELECT/INSERT y trigger contra UPDATE/DELETE.|
|TAC-003|RF-002, RF-003, RF-008, ATC-004, ATC-008|Gestionar cupos por fila advisor_slot y usar blo- queo transaccional (SELECT FOR UPDATE o equivalente), restricción de integridad y pruebas de concurrencia.|
|TAC-004|RF-004, RF-005, ATC-001, ATC-008|Transactional Outbox, RabbitMQ, consumidores idempotentes, reintentos con backoff y DLQ. Para cierre, cancelar en Appointment Service y propa- gar notifyCustomer=false; reconciliar eventos falli- dos.|
|TAC-005|RF-006, RF-007, ATC-002, ATC-008|OIDC/JWT, RBAC, mínimo privilegio, TLS, vali- dación de entrada, CORS restrictivo, secretos me- diante variables/secret store, DB users separados y no exposición de puertos de datos.|
|TAC-006|ATC-003|Formularios reactivos con validación de cliente y servidor; mostrar estado, razón de reapertura, ca- pacidad y citas; listas paginadas y mensajes de er- ror accionables.|
|TAC-007|ATC-005, RF-010|Arquitectura por capas/hexagonal dentro de cada servicio, contratos OpenAPI, migraciones Flyway, pruebas unitarias e integración y logs estructura- dos con correlationId.|
|TAC-008|ATC-006|Consultas paginadas e índices por estado, fecha, matrícula, orderId y advisorId; evitar cargar histori- ales completos en la vista principal.|
|TAC-009|RF-010, ATC-001, ATC-005||

|ID|ID relacionado|Táctica asociada|
|---|---|---|
|||Restringir consulta de auditoría a roles autorizados y exponer registros en orden cronológico con cur- sor/paginación; registrar acceso administrativo cuando aplique.|
|TAC-010|RF-008, ATC-004, ATC-003|Health/readiness checks, reinicio controlado, métricas de consumidor, colas, tiempos y errores; alertar sobre mensajes en DLQ y acumulación de outbox.|
|TAC-011|ATC-002, ATC-006|Versionar contrato de integración, validar es- quema, autenticar sistema externo, mapear IDs externos a internos y hacer deduplicación por ase- guradora + externalReference.|
|TAC-012|ATC-007, RF-009||

##### 1.13.2 Patrones

Se evalúan, como mínimo, patrones arquitectónicos, de diseño, seguridad, datos y presentación, según la guía. *Tabla 9. Patrones de arquitectura y diseño*

|ID|Tipo|Descripción|
|---|---|---|
|||Microservicios por bounded context: Order Ser- vice, Appointment Service y Notification Worker. APIs independientes y propiedad de datos por ser- vicio.|
|PAT-001|Arquitectura / Microservicios|API Gateway para el acceso web; REST síncrono para comandos/consultas y RabbitMQ Pub- lish/Subscribe para eventos.|
|PAT-002|Arquitectura / Integración|Transactional Outbox + consumidores idempo- tentes; reintentos, DLQ y reconciliación para en- trega al menos una vez sin duplicar efectos de ne- gocio.|
|PAT-003|Integración / Resiliencia|Database per Service: bases separadas y sin con- sultas o joins entre servicios. Las referencias cruzadas son identificadores externos validados por API/evento.|
|PAT-004|Datos|Historial append-only para estados, con registro transaccional, permisos y trigger anti-mutación; hash chain para detectar cambios y evolución a WORM independiente.|
|PAT-005|Datos / Seguridad|State Machine para transiciones de orden; Reposi- tory y Unit of Work/transaction boundary para per- sistencia consistente.|
|PAT-006|Diseño||
|PAT-007|Diseño / Concurrencia|Pessimistic locking sobre advisor_slot para garan- tizar cupos bajo solicitudes concurrentes. OIDC, JWT, RBAC y mínimo privilegio; el servidor deriva el actor del token, no de campos manipula- bles del cliente.|
|PAT-008|Seguridad|SPA Angular con separación por funcionalidades y formularios reactivos; frontend no accede directa- mente a datos.|
|PAT-009|Presentación|Migraciones versionadas, OpenAPI, pruebas au- tomatizadas, configuración externa y observabili- dad desde el inicio.|
|PAT-010|Gobierno / Desarrollo||

##### 1.13.3 Frameworks

*Tabla 10. Frameworks y componentes habilitadores*

|ID|Nombre|Tipo|Respaldo / uso|
|---|---|---|---|
||||SPA web, componentes, routing, for- mularios reactivos y HttpClient; inter- faz del asesor/supervisor/auditor.|
|FRM-001|Angular|Framework frontend|Construcción de servicios REST, in- yección de dependencias, validación, configuración y Actuator.|
|FRM-002|Java + Spring Boot|Framework backend||
|FRM-003|Spring Cloud Gateway|API Gateway|Enrutamiento hacia los microservi- cios y filtros comunes.|
|FRM-004|Spring Security|Seguridad|Validación de JWT/OIDC y autor- ización basada en roles/policies.|

|ID|Nombre|Tipo|Respaldo / uso|
|---|---|---|---|
||||Repositorios y mapeo objeto-rela- cional; transacciones controladas por servicio.|
|FRM-005|Spring Data JPA / Hibernate|Persistencia ORM|Persistencia relacional y transac- ciones ACID; bases/roles independi- entes por servicio.|
|FRM-006|PostgreSQL|RDBMS||
|FRM-007|RabbitMQ + Spring AMQP|Mensajería|Publicación y consumo de eventos, reintentos y DLQ.|
|FRM-008|Flyway|Migraciones|Control versionado del esquema por servicio.|
|FRM-009|OpenAPI 3|Contrato API|Documentar endpoints, DTOs y ver- sionamiento. Construcción/ejecución de imágenes OCI y levantamiento local con `pod- man compose up --build`.|
|FRM-010|Podman + Compose provider|Runtime / despliegue local|Autenticación de usuarios, emisión de tokens y roles. La selección debe validarse con el entorno.|
|FRM-011|Keycloak (o IdP OIDC equivalente)|Identidad||
|FRM-012|Nginx|Servidor web / proxy estático|Servir el build de Angular en el con- tenedor frontend.|

Los números de versión exactos deben fijarse en el repositorio mediante archivos de build y probarse contra el runtime Podman disponible. No se fijan precios unitarios porque no fueron suministrados ni son necesarios para la prueba.

#### 1.14 Vistas de la arquitectura

Las vistas se estructuran siguiendo el enfoque 4+1 descrito en la guía. Los diagramas editables se entregan en la car- peta `diagramas/`. Los archivos PNG embebidos son vistas de lectura dentro de este documento; Draw.io es la fuente editable. Las vistas describen el estado TO-BE. El AS-IS corresponde a Excel y WhatsApp como medios operativos ac- tuales.

##### 1.14.1 Vista lógica

La vista lógica representa conceptos del dominio y relaciones sin imponer detalles de infraestructura. La orden agrupa cliente, vehículo, aseguradora opcional, ítems de trabajo y eventos de estado. Appointment es un agregado del servicio de citas que referencia el orderId de forma lógica, no mediante una clave foránea entre bases.

*Ilustración 1. Modelo de clases del dominio*

La relación discontinua entre WorkOrder y Appointment es una referencia lógica: Appointment Service valida la orden mediante API y recibe eventos de cierre. Los historiales de estado pertenecen al agregado de orden.

##### Modelo lógico relacional

El modelo separa `orders_db` y `appointments_db`, incluso si ambas bases residen en la misma instancia PostgreSQL durante el desarrollo. Las tablas `outbox_events` existen en cada contexto. `processed_events` permite deduplicar eventos entrantes. El modelo es lógico y no pretende sustituir un modelo físico final con tipos, longitudes e índices definitivos.

*Ilustración 2. Modelo lógico relacional por servicio*

Las relaciones sólidas son internas a una base/servicio. La relación entre work_orders y appointments es externa, sin FK cross-database. Los detalles de tipos, índices y particionado se decidirán con los volúmenes reales.

##### 1.14.2 Vista de desarrollo

La solución separa la interfaz Angular de las reglas de negocio. El gateway enruta solicitudes autenticadas. Order Service contiene las reglas de ciclo de vida y auditoría; Appointment Service aplica reservas y cancelaciones; Notification Worker consume eventos y aplica la política de mensajes. Ningún frontend o servicio de negocio consulta directamente la base de datos de otro servicio.

*Ilustración 3. Diagrama de componentes*

La llamada desde Appointment Service hacia el endpoint interno de Order Service se limita a verificar elegibilidad de la orden. El resto de interacciones entre dominios se comunica mediante eventos. El componente de integración de aseguradoras se mantiene como frontera futura.

#### 1.15 Vista de procesos

La vista de procesos muestra la secuencia y las reglas en ejecución. Las operaciones de escritura de cada dominio se confirman localmente; los efectos en el otro dominio se procesan de forma asíncrona mediante outbox y RabbitMQ.

TALLER 360 SUBOCOL | DISEÑO DE ARQUITECTURA
Secuencia 1 - Registro / cambio de estado y auditoría
Asesor Angular API Gateway Order Service PostgreSQL RabbitMQ
Completa datos / selecciona nuevo estado
POST /oirders o PATCH /orders/{id}/status + JWT
Valida JWT, rol y enruta
BEGjN: valida transición y persiste orden
INSERT historial append-only + hash + outbox
COMMIT atómico
Outbox publisheir publica evento
Respuesta 201 / 200 con orderld y estado
Respuesta JSON
Lineas continuas: llamada/respuesta sincrona. Líneas discontinuas: evento asincrono.
llustración 4. Secuencia de registro/cambio de estado y auditoría
El cambio de estado, el evento de historial y el registro Outbox quedan dentro de una única transacción de Order Ser-
vice. El publicador envía el evento después del commit. Si falla la escritura del historial, la transición completa se re-
vierte.
Secuencia 2 - Agendamiento y notificación
Asesor Angular Gateway Appointment Service Order Service PostgreSQL RabbitMQ Notify Worker
Seleccions orden, asesor y frarje
POST /appointments + JWT
Valida JWT y enruta
GET elegibilidad de la orden
Orden válida / no cerrada
Bloquea slot verifice cupo; inserta cita
COMMIT + outbox AgpointmentScheduled
Publica AppointmentScheduleil
Entrega evento
Registra log: WhatsApp SIMULADO SENT
201 Created
Confirmación de cita
Linmes continuas: llamsda/respuesta sincrons. Linews discontinuas: #uento asincrtno
Ilustración 5. Secuencia de agendamiento y notificación
Propuesta para prueba técnica Subocol • Página 13

Appointment Service valida que la orden exista y esté activa, bloquea la franja de agenda, verifica la capacidad y registra la cita. La confirmación de cita se publica por Outbox. Notification Worker consume el evento y escribe el log simulado.

*Ilustración 6. Secuencia de cierre y cancelación silenciosa*

La orden se cierra primero de forma transaccional. Después, Appointment Service consume OrderClosed y cancela las citas pendientes. El evento de cancelación lleva `notifyCustomer=false`; Notification Worker no escribe un envío de WhatsApp. Existe consistencia eventual entre servicios.

*Ilustración 7. Secuencia de reapertura con motivo obligatorio*

Solo un rol autorizado puede ejecutar la reapertura. El servicio rechaza solicitudes sin motivo, registra la transición y conserva el evento histórico. Las citas que se cancelaron al cerrar no se reactivan de forma automática.

TALLER 360 SUBOCOL L DISENO DE ARQUITECTURA
Secuencia 5 - Cancelación manual v notificación
Acesor Angular API Gateway Appointment Service PostgreSQL RabbitMQ Notify Worker
Solicita cancelar una cita pendiente
POST /appointments/{id}/cancel  JWT
Valida identidad, rol y enruta)
Verifica que la cita esté PENDING
UPDATE CANCELLED + outbox notifyCustomer=true
COMMIT
Publica AppointmentCancelled
Entrega evento
Escribe log WhatsApp SIMULADO SENT
200 OK
Confirmación de cancelación
Lineas continuas: Illamada/respuesta sincrona. Lineas discontinuas: evento asincrono.
llustración 8. Secuencia de cancelación manual y notificación
A diferencia de la cancelación automática por cierre, una cancelación manual de una cita pendiente genera Appoint-
mentCancelled con notifyCustomer=true. El Notification Worker consume el evento y escribe el loq simulado de What-
sApp.
Propuesta para prueba técnica Subocol • Página 15

TALLER 360 SUBOCOL | DISEÑO DE ARQUITECTURA
Vista de procesos - flujo de cierre y cancelación
Asesor solicita cerrar orden
Order Service valida transición
Transacción: CLOSED + historial + outbox
Publica OrderClosed en RabbitMQ
Appointment Service consume evento
(idempotencia)
Cancela citas pendientes
reason=ORDER CLOSED
notifyCustomer=false
Publica AppointmentCancelled
Notification Worker evalúa política
SUPRIMIR: no emitir WhatsApp
Propuesta para prueba técnica Subocol• Página 16

*Ilustración 9. Actividad de cierre y cancelación automática*

La secuencia resalta el comportamiento que no debe romperse: el cierre de la orden provoca cancelación de citas pendientes y la política de notificación suprime el mensaje. El consumidor debe soportar reentregas sin duplicar cambios.

##### 1.15.1 Vista física

Para desarrollo local, los contenedores se ejecutan con Podman en una red privada. Solo se publica el frontend y, cuando se requiera, puertos de administración locales para gateway, broker e identidad. Los microservicios, PostgreSQL y RabbitMQ no deben exponerse directamente a redes públicas. Las credenciales y volúmenes son gestionados fuera de la imagen.

*Ilustración 10. Diagrama de despliegue local con Podman*

La topología usa una instancia PostgreSQL con dos bases y credenciales independientes para simplificar el entorno de prueba sin romper la propiedad de datos. Los puertos de administración solo deben enlazarse a localhost. La zona de stdout representa la simulación de WhatsApp.

Comandos de referencia para el entorno local (requieren Podman y un provider Compose instalado): podman compose up --build podman compose down

Si `podman compose` informa que no encuentra provider, instalar/configurar un provider compatible, por ejemplo `podman-compose`, y ejecutar `podman-compose up --build`. El archivo Compose debe usar especificación portable, imágenes OCI y volúmenes nombrados; no debe depender de funciones exclusivas de Docker Desktop. En Windows puede ser necesaria Podman Machine y virtualización habilitada. El funcionamiento real debe probarse en el host del candidato.

Puertos de referencia de desarrollo: frontend 8080, gateway 8081, Keycloak 8180, RabbitMQ 5672 (15672 administración local), PostgreSQL 5432. Los puertos internos de servicios (8082/8083) permanecen privados en la red Compose.

##### 1.15.2 Vista de escenarios

Los casos de uso validan que las funcionalidades descritas por los RF se reflejen en la estructura lógica, de desarrollo, procesos y despliegue. Los actores internos trabajan desde Angular; el auditor consulta el historial sin permisos de escritura; el sistema externo de aseguradora aparece únicamente como actor futuro.

*Ilustración 11. Diagrama de casos de uso*

Los casos de agendamiento incluyen validación de capacidad. Las cancelaciones automáticas se disparan por cierre de orden y no ejecutan el caso de uso de notificación. La integración de aseguradoras se marca como futura y separada del flujo del asesor.

#### 1.16 Ambientes implementados

Se definen los tres ambientes obligatorios que establece la guía. La prueba describe una ejecución local; por tanto, Desarrollo es el ambiente implementable de inmediato y Pruebas/Producción se describen como destinos lógicos pendientes de infraestructura definitiva. *Tabla 11. Ambientes del sistema*

|ID|Nombre|Actividades|
|---|---|---|
|||Ejecución local con Podman Compose; vari- ables de entorno de desarrollo; Keycloak con usuarios/roles de prueba; datos sintéticos; WhatsApp simulado por stdout; crear ramas por historia y ejecutar pruebas unitarias.|
|AMB-01|Desarrollo|Despliegue aislado con configuración de prueba; pruebas de transición de estados, reapertura, concurrencia de cupos, outbox/rein- tentos, DLQ y supresión de notificaciones. Pruebas de integración Angular/API y seguri- dad.|
|AMB-02|Pruebas / QA|Infraestructura y proveedor por definir. Secre- tos gestionados, TLS, monitoreo y alertas, backups probados, políticas de retención, con- trol de accesos y operación de colas. What- sApp y aseguradoras solo tras aprobación de sus integraciones reales.|
|AMB-03|Producción||

#### 1.17 Dimensionamiento-crecimiento

El caso no proporciona número de órdenes diarias, concurrencia, tamaño de payload, retención ni crecimiento por- centual. No es responsable fijar capacidad final sin estos datos. La tabla define métricas a medir y una fórmula inicial para planificación. *Tabla 12. Estimación y variables de crecimiento*

|Variable|Estado|Uso en dimensionamiento|
|---|---|---|
|Órdenes/día y citas/día|Pendiente de medición|Volumen transaccional y tamaño de tablas prin- cipales.|
|Concurrencia máxima de reservas por franja|Pendiente de medición|Dimensionar conexiones y validar bloqueo de cupos bajo carga.|
|Tamaño medio de orden + ítems + historial|Pendiente de medición|Calcular crecimiento de orders_db.|
|Retención del historial|Pendiente de política|Determina crecimiento y estrategia de archivo/retención.|
|Mensajes/eventos por día y retención de colas|Pendiente de medición|Dimensionar RabbitMQ, DLQ y monitoreo.|
|Archivos binarios|No se requieren en MVP|No se reserva almacenamiento documental mientras el caso no pida adjuntos.|

Fórmula preliminar: crecimiento anual de datos ≈ órdenes/día × 365 × bytes promedio por orden (incluidos ítems e historial) × factor de índices/overhead. Debe calcularse por separado para citas y outbox; el crecimiento de logs se gestiona con rotación y retención. Tomar mediciones reales en QA antes de dimensionar Producción.

#### 1.18 Componentes habilitadores del desarrollo

Se enumeran componentes definidos para implementar la solución. Los precios/licenciamiento exactos dependen de versiones, modalidad y política de la organización; no se atribuye un precio no suministrado. *Tabla 13. Habilitadores de desarrollo*

|Componente|Uso|Arquitectura de referencia|Licencia / costo unitario|
|---|---|---|---|
|Angular Java / Spring Boot / Spring Cloud Gateway|Frontend SPA. Servicios backend y gateway.|Tecnología requerida por el caso. Tecnología Java requerida; frame- works seleccionados para la solu- ción.|OSS; validar política/versión. OSS; validar versiones y dependen- cias.|
|PostgreSQL|Persistencia relacional.|Componente de datos seleccionado.|OSS; costos de infraestructura no in- cluidos.|
|RabbitMQ Keycloak|Mensajería asíncrona. Identidad OIDC / roles.|Componente de integración selec- cionado. Componente de seguridad prop- uesto; IdP equivalente sujeto a vali- dación.|OSS; costos operativos no incluidos. OSS; operación/soporte no incluidos.|
|Podman + Compose provider Flyway / OpenAPI|Runtime de contenedores local. Migraciones y contrato API.|Sustituto de Docker solicitado por el candidato. Herramientas de habilitación de de- sarrollo.|OSS; requisitos de virtualización según host. Ediciones/complementos por validar; funciones básicas disponibles en OSS.|
|Nginx|Servir frontend compilado.|Componente de presentación/de- spliegue.|OSS.|

#### 1.19 Riesgos de la arquitectura

*Tabla 14. Riesgos de arquitectura*

|ID|Descripción|Probabilidad|Impacto|Mitigación|
|---|---|---|---|---|
||Evento OrderClosed no se procesa y quedan citas pen- dientes.|||Transactional Outbox, rein- tentos, DLQ, alertas y pro- ceso de reconciliación por órdenes cerradas con citas activas.|
|RSK-01|Dos asesores reservan si- multáneamente el último cupo.|Media|Alto|Bloqueo de fila/actualización condicional dentro de una transacción y pruebas de concurrencia.|
|RSK-02||Media|Alto|Separar credenciales, re- stringir UPDATE/DELETE y triggers, hash chain, backup|
|RSK-03|Privilegio administrativo al- tera el historial de auditoría.|Baja/Media|Crítico||

|ID|Descripción|Probabilidad|Impacto|Mitigación|
|---|---|---|---|---|
||Cierre de orden y can- celación de citas muestran estados temporalmente dis- tintos.|||y futura réplica WORM en dominio administrativo inde- pendiente. Aceptar consistencia even- tual explícita, mostrar estado de procesamiento si se necesita, reintentar y recon- ciliar. Objetivo propuesto <10 s.|
|RSK-04|Logs/notificaciones dupli- cadas por reentrega de Rab- bitMQ.|Media|Medio|Consumidor idempotente, eventId único y registro de evento procesado.|
|RSK-05|Proveedor real de WhatsApp o contrato de aseguradora cambia/no está disponible.|Media|Medio|Adaptadores desacoplados, contratos versionados, time- out/retry y no acoplar do- minio a proveedor.|
|RSK-06|Podman Compose no fun- ciona por incompatibilidad de host o ausencia de virtual- ización/provider.|Alta en integración futura|Medio|Validar instalación en primer momento, documentar provider, comprobar Podman Machine y mantener Com- pose portable.|
|RSK-07|Reglas de estados, capaci- dad o roles incompletas cau- san comportamiento incor- recto.|Media|Alto para evaluación|Validar catálogo de estados, matriz de transición, reglas de franja/cupo y roles con dueño funcional antes de cerrar implementación.|
|RSK-08|Exposición indebida de datos personales del cliente/vehículo.|Alta|Alto|Mínimo privilegio, TLS, se- cretos externos, enmas- caramiento en logs, control de acceso y política de re- tención aprobada.|
|RSK-09|Volumen real supera los supuestos de una instancia PostgreSQL/RabbitMQ local.|Media|Alto|Capturar métricas de carga; ajustar recursos, índices, pooling y retención tras pi- loto.|
|RSK-10||Baja (sin medición)|Medio||

### 2. Conclusiones

Taller 360 Subocol cubre los procesos prioritarios del caso con dos microservicios de dominio, persistencia separada, un API Gateway y mensajería asíncrona. El diseño mantiene el ciclo de vida de la orden independiente de la agenda, protege la capacidad por asesor y evita que una indisponibilidad temporal del servicio de citas bloquee el cierre de una orden.

La auditoría se registra en la misma transacción que cada cambio de estado. Los permisos append-only, los triggers y la cadena hash elevan la protección y la capacidad de detección, pero no garantizan inviolabilidad frente a un supe- rusuario que controle la base. Para una exigencia probatoria fuerte, la arquitectura debe incorporar un almacenamiento WORM bajo administración independiente.

Podman reemplaza Docker para el entorno de desarrollo y conserva la definición Compose como contrato operativo. Su ejecución real depende del host, virtualización y provider Compose. Las integraciones productivas con aseguradoras y WhatsApp están reservadas para fases posteriores, no forman parte del MVP de un día.

Antes de aprobar el diseño para construcción, validar con los responsables del taller:

· Catálogo de estados, transiciones permitidas y roles autorizados para reabrir o cerrar órdenes. · Definición de cita pendiente, duración de franjas, reglas de solapamiento y configuración de cupos. · Volúmenes, concurrencia pico, retención, objetivos de disponibilidad y recuperación. · Nivel de inmutabilidad/auditoría exigido frente a administradores privilegiados y necesidad de WORM externo.