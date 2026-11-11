#!/usr/bin/env bash
# Verificación de punta a punta del MVP: levanta el stack y comprueba, con
# peticiones reales y SQL real, que lo que se promise se sostiene.
#
#   ./scripts/verify-mvp.sh
#
# Requiere el stack arriba (`podman compose up --build`).

set -uo pipefail

GATEWAY="http://localhost:8081"
FRONTEND="http://localhost:8080"
OK=0
FAIL=0

check() {
  local name="$1" expected="$2" actual="$3"
  if [ "$actual" = "$expected" ]; then
    printf '  OK   %-58s %s\n' "$name" "$actual"
    OK=$((OK + 1))
  else
    printf '  FALLA %-58s esperado=%s obtenido=%s\n' "$name" "$expected" "$actual"
    FAIL=$((FAIL + 1))
  fi
}

contains() {
  local name="$1" needle="$2" haystack="$3"
  if printf '%s' "$haystack" | grep -q "$needle"; then
    printf '  OK   %-58s contiene %s\n' "$name" "$needle"
    OK=$((OK + 1))
  else
    printf '  FALLA %-58s no contiene %s\n' "$name" "$needle"
    FAIL=$((FAIL + 1))
  fi
}

echo "== 1. Contenedores y salud =="
for svc in postgres rabbitmq order-service appointment-service notification-worker gateway frontend; do
  state=$(podman inspect --format '{{.State.Status}}/{{if .State.Health}}{{.State.Health.Status}}{{else}}sin-healthcheck{{end}}' "subocol-$svc" 2>/dev/null)
  check "subocol-$svc" "running/healthy" "$state"
done

echo
echo "== 2. El frontend sirve la pagina =="
status=$(curl -s -o /dev/null -w '%{http_code}' "$FRONTEND/")
check "GET $FRONTEND/" "200" "$status"

echo
echo "== 3. Registrar una orden por el gateway =="
payload='{"customerName":"Ana Ruiz","vehiclePlate":"ABC123","vehicleDescription":"Toyota Corolla 2018","insurerCode":null}'
created=$(curl -s -X POST "$GATEWAY/api/orders" -H 'Content-Type: application/json' -d "$payload")
contains "POST /api/orders responde 201 con id" '"orderId"' "$created"
contains "la orden nace en RECIBIDA" '"status":"RECIBIDA"' "$created"
order_id=$(printf '%s' "$created" | sed -n 's/.*"orderId":"\([^"]*\)".*/\1/p')
echo "  orden creada: $order_id"

echo
echo "== 4. Listar y ver el detalle con historial =="
listing=$(curl -s "$GATEWAY/api/orders?page=0&size=20")
contains "GET /api/orders lista la orden" "$order_id" "$listing"
detail=$(curl -s "$GATEWAY/api/orders/$order_id")
contains "el detalle trae el historial" '"history"' "$detail"
contains "el historial arranca en RECIBIDA" '"toStatus":"RECIBIDA"' "$detail"
contains "el previousHash inicial es de 64 ceros" \
  '"previousHash":"0000000000000000000000000000000000000000000000000000000000000000"' "$detail"

echo
echo "== 5. Aislamiento entre dominios =="
cross=$(podman exec subocol-postgres psql -U orders_user -d appointments_db -c 'select 1;' 2>&1)
contains "orders_user no entra a appointments_db" 'permission denied' "$cross"
cross2=$(podman exec subocol-postgres psql -U appointment_user -d orders_db -c 'select 1;' 2>&1)
contains "appointment_user no entra a orders_db" 'permission denied' "$cross2"

echo
echo "== 6. El historial es append-only =="
mutate=$(podman exec subocol-postgres psql -U orders_owner -d orders_db \
  -c "update order_status_history set reason = 'manipulado';" 2>&1)
contains "UPDATE sobre el historial falla" 'append-only' "$mutate"
delete=$(podman exec subocol-postgres psql -U orders_owner -d orders_db \
  -c 'delete from order_status_history;' 2>&1)
contains "DELETE sobre el historial falla" 'append-only' "$delete"

echo
echo "== 7. La cadena de hash se verifica =="
chain=$(podman exec subocol-postgres psql -U orders_owner -d orders_db -t -A \
  -c "select count(*) from order_status_history where length(event_hash) = 64 and previous_hash = repeat('0', 64);")
if [ "${chain:-0}" -ge 1 ]; then
  printf '  OK   %-58s %s\n' "filas con hash de 64 y genesis de ceros" "$chain"
  OK=$((OK + 1))
else
  printf '  FALLA %-58s esperado>=1 obtenido=%s\n' "filas con hash de 64 y genesis de ceros" "$chain"
  FAIL=$((FAIL + 1))
fi

echo
echo "== Resultado =="
printf '  %d comprobaciones OK, %d fallidas\n' "$OK" "$FAIL"
[ "$FAIL" -eq 0 ]
