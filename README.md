# Taller 360 Subocol - Sistema de Gestión de Órdenes y Agenda

## 📌 Descripción de la Solución
Solución basada en microservicios orientada a eventos para la gestión de órdenes de servicio automotrices, control concurrente de citas y notificaciones simuladas.

## 🛠️ Requisitos Previos
- **Podman** & **Podman Compose**
- **Java 25 / Spring Boot 4.x**
- **Node.js & Angular CLI**

## 🚀 Paso a Paso de Ejecución Local

1. **Clonar el repositorio:**
   ```bash
   git clone https://github.com/JuanBM0308/subocol.git
   cd subocol

## 🚀 Ejecución local del MVP (Podman)

```bash
podman compose up --build      # o: podman-compose up --build
```

| Qué | Dónde |
|-----|-------|
| Frontend (registrar orden, listado, detalle) | http://localhost:8080 |
| Gateway (API de órdenes) | http://localhost:8081/api/orders |
| Management de RabbitMQ | http://localhost:15672 (usuario `workshop`) |
| PostgreSQL y RabbitMQ | solo dentro de `workshop-net`; no se publican |

Requisitos: Podman 6+ con la máquina WSL arrancada (`podman machine start`) y un
proveedor de compose (`podman-compose` en el `PATH`).

Notas:

- Las credenciales salen de variables de entorno con valores de ejemplo. Copia
  `.env.example` a `.env` si quieres cambiarlas; `.env` no se versiona.
- Los scripts de `db/init/` se ejecutan **una sola vez**, cuando el volumen de
  PostgreSQL está vacío. Para rehacer la base desde cero:
  `podman compose down -v` (borra los volúmenes).
- Cada base tiene su propio rol de aplicación y su propio rol propietario (solo
  migraciones). Un rol de un dominio no puede conectarse a la base del otro.
- `order_status_history` es append-only: un trigger rechaza `UPDATE` y `DELETE`.
