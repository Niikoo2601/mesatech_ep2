#!/usr/bin/env bash
# =====================================================================
# Crea (o vuelve a aplicar) los recursos de mensajería de MesaTech
# usando la API HTTP de administración de RabbitMQ (puerto 15672).
#
# Ejecutar EN LA EC2 C, en la misma carpeta que compose.yaml y .env,
# después de "docker compose up -d" y de que el contenedor esté healthy:
#
#     chmod +x configurar-recursos.sh
#     ./configurar-recursos.sh
#
# Es idempotente: se puede ejecutar varias veces sin duplicar nada.
# Crea:
#   - virtual host   mesatech
#   - exchange       catalogo.exchange        (direct, durable)
#   - cola           solicitudes.catalogo.queue (classic, durable, sin autoeliminación)
#   - binding        exchange -> cola con routing key categoria.creada
#   - usuario        catalogo_publicador    (solo puede ESCRIBIR en el exchange)
#   - usuario        solicitudes_consumidor (solo puede LEER la cola)
# =====================================================================
set -euo pipefail

cd "$(dirname "$0")"

if [[ ! -f .env ]]; then
  echo "No existe .env. Copie .env.example a .env y complete las credenciales." >&2
  exit 1
fi

set -a
# shellcheck disable=SC1091
source .env
set +a

API="${RABBITMQ_API:-http://localhost:15672/api}"
VHOST="${RABBITMQ_VHOST:-mesatech}"
EXCHANGE="${RABBITMQ_EXCHANGE:-catalogo.exchange}"
QUEUE="${RABBITMQ_QUEUE:-solicitudes.catalogo.queue}"
ROUTING_KEY="${RABBITMQ_ROUTING_KEY:-categoria.creada}"

# Convierte texto a JSON de forma segura (contraseñas con caracteres especiales)
json_str() { python3 -c 'import json,sys; print(json.dumps(sys.argv[1]))' "$1"; }
# Codifica el virtual host para la URL
url() { python3 -c 'import sys,urllib.parse; print(urllib.parse.quote(sys.argv[1], safe=""))' "$1"; }
# Escapa un nombre para usarlo como expresión regular exacta en permisos
regex_exacta() { python3 -c 'import re,sys; print("^" + re.escape(sys.argv[1]) + "$")' "$1"; }

api() {
  local metodo="$1" ruta="$2" cuerpo="${3:-}"
  local codigo
  if [[ -n "$cuerpo" ]]; then
    codigo=$(curl -sS -o /tmp/rabbit_api_resp -w '%{http_code}' -u "$RABBITMQ_ADMIN_USER:$RABBITMQ_ADMIN_PASSWORD" \
      -X "$metodo" -H 'content-type: application/json' -d "$cuerpo" "$API$ruta")
  else
    codigo=$(curl -sS -o /tmp/rabbit_api_resp -w '%{http_code}' -u "$RABBITMQ_ADMIN_USER:$RABBITMQ_ADMIN_PASSWORD" \
      -X "$metodo" "$API$ruta")
  fi
  if [[ "$codigo" != 2* ]]; then
    echo "ERROR $codigo en $metodo $ruta: $(cat /tmp/rabbit_api_resp)" >&2
    exit 1
  fi
}

V=$(url "$VHOST")

echo "1) Virtual host '$VHOST'"
api PUT "/vhosts/$V" '{"description":"MesaTech Cloud - eventos de catálogo"}'

echo "2) Permisos del administrador sobre '$VHOST' (para gestionar la topología)"
api PUT "/permissions/$V/$(url "$RABBITMQ_ADMIN_USER")" '{"configure":".*","write":".*","read":".*"}'

echo "3) Exchange '$EXCHANGE' (direct, durable)"
api PUT "/exchanges/$V/$(url "$EXCHANGE")" '{"type":"direct","durable":true,"auto_delete":false,"internal":false,"arguments":{}}'

echo "4) Cola '$QUEUE' (classic, durable, sin autoeliminación)"
api PUT "/queues/$V/$(url "$QUEUE")" '{"durable":true,"auto_delete":false,"arguments":{"x-queue-type":"classic"}}'

echo "5) Binding '$EXCHANGE' -> '$QUEUE' con routing key '$ROUTING_KEY'"
api POST "/bindings/$V/e/$(url "$EXCHANGE")/q/$(url "$QUEUE")" "{\"routing_key\":$(json_str "$ROUTING_KEY"),\"arguments\":{}}"

echo "6) Usuario publicador '$RABBITMQ_PUBLICADOR_USER' (sin tags administrativos)"
api PUT "/users/$(url "$RABBITMQ_PUBLICADOR_USER")" "{\"password\":$(json_str "$RABBITMQ_PUBLICADOR_PASSWORD"),\"tags\":\"\"}"

echo "7) Usuario consumidor '$RABBITMQ_CONSUMIDOR_USER' (sin tags administrativos)"
api PUT "/users/$(url "$RABBITMQ_CONSUMIDOR_USER")" "{\"password\":$(json_str "$RABBITMQ_CONSUMIDOR_PASSWORD"),\"tags\":\"\"}"

# Permisos mínimos (expresiones regulares sobre nombres de recursos del vhost):
#   publicador: configure=nada, write=solo el exchange, read=nada
#   consumidor: configure=nada, write=nada,            read=solo la cola
echo "8) Permisos mínimos en '$VHOST'"
api PUT "/permissions/$V/$(url "$RABBITMQ_PUBLICADOR_USER")" \
  "{\"configure\":\"^\$\",\"write\":$(json_str "$(regex_exacta "$EXCHANGE")"),\"read\":\"^\$\"}"
api PUT "/permissions/$V/$(url "$RABBITMQ_CONSUMIDOR_USER")" \
  "{\"configure\":\"^\$\",\"write\":\"^\$\",\"read\":$(json_str "$(regex_exacta "$QUEUE")")}"

echo
echo "Recursos configurados. Verificación:"
echo "--- Permisos en $VHOST"
curl -sS -u "$RABBITMQ_ADMIN_USER:$RABBITMQ_ADMIN_PASSWORD" "$API/vhosts/$V/permissions" | python3 -c "
import json, sys
for p in json.load(sys.stdin):
    print('  %-26s configure=%-6s write=%-24s read=%s' % (p['user'], p['configure'], p['write'], p['read']))
"
echo "--- Bindings del exchange"
curl -sS -u "$RABBITMQ_ADMIN_USER:$RABBITMQ_ADMIN_PASSWORD" "$API/exchanges/$V/$(url "$EXCHANGE")/bindings/source" | python3 -c "
import json, sys
for b in json.load(sys.stdin):
    print('  %s --[%s]--> %s' % (b['source'], b['routing_key'], b['destination']))
"
echo "--- Cola"
curl -sS -u "$RABBITMQ_ADMIN_USER:$RABBITMQ_ADMIN_PASSWORD" "$API/queues/$V/$(url "$QUEUE")" | python3 -c "
import json, sys
q = json.load(sys.stdin)
print('  %s durable=%s auto_delete=%s ready=%s unacked=%s' % (q['name'], q['durable'], q['auto_delete'], q.get('messages_ready', 0), q.get('messages_unacknowledged', 0)))
"
