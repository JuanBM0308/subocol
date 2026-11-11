**El problema:** un taller automotriz recibe vehículos de clientes particulares y de aseguradoras. Hoy todo se maneja en Excel y WhatsApp. Necesitan un sistema donde:

- El asesor registra la orden de servicio: cliente, vehículo, aseguradora si aplica y qué hay que hacer.
- La orden avanza por estados hasta repararse y cerrarse. A veces hay que reabrirla, y siempre con una razón.
- El cliente tiene citas para traer o recoger el vehículo. Cada asesor atiende un número limitado de citas por franja horaria.
- El cliente recibe un WhatsApp cuando se le agenda o cancela una cita.
- Al cerrar una orden, sus citas pendientes se cancelan solas, y en ese caso no se le avisa al cliente, porque ya recogió su vehículo.
- Auditoría pide saber quién cambió cada estado y cuándo, sin que nadie pueda alterar ese registro.
- Las órdenes de las aseguradoras grandes llegarán más adelante por integración, no las crea el asesor. Restricciones técnicas: front end Angular, backend en Java con microservicios y todo levantando con docker compose up. El WhatsApp se puede simular con un log. Tú decides cuántos servicios hay, qué hace cada uno, de quién es cada dato y cómo se comunican. No tienes que construir todo: prioriza y justifica qué dejaste fuera. Entrega:
- El repo.
- Un diagrama de contexto con los dominios y cómo se hablan.
- Un DECISIONES.md con cada frontera que trazaste, las alternativas que descartaste y al menos 2 propuestas de la IA que rechazaste. Tiempo: 1 día. Pueden usar IA libremente.