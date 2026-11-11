#!/usr/bin/env bash
# Siembra ordenes de demostracion para probar el flujo en el navegador.
#
#   ./scripts/seed-demo-data.sh
#
# Las ordenes se registran por la API (frontend -> nginx -> gateway -> order-service),
# asi que cada una queda con su fila de historial y su hash encadenado.

set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"

create_order() {
  curl -s -X POST "$BASE_URL/api/orders" \
    -H 'Content-Type: application/json' \
    -d "{\"customerName\":\"$1\",\"vehiclePlate\":\"$2\",\"vehicleDescription\":\"$3\",\"insurerCode\":$4}" \
    && echo
}

echo "Registrando ordenes de demostracion en $BASE_URL ..."
create_order "Ana Ruiz" "ABC123" "Toyota Corolla 2018 - cambio de aceite y filtros" "null"
create_order "Carlos Mendez" "XYZ987" "Mazda CX-5 2021 - revision general y alineacion" '"SUR-001"'
create_order "Laura Gomez" "LMN456" "Volkswagen Golf 2019 - suspension y frenos" "null"

echo
echo "Listado actual:"
curl -s "$BASE_URL/api/orders?page=0&size=20"
echo
