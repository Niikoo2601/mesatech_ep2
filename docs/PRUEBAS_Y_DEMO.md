# Pruebas P1 a P8, guion de demo y preparación de la defensa

Para cada prueba, el informe debe registrar: **preparación, datos usados, resultado esperado, resultado
obtenido y evidencia** (número de captura). La plantilla de tabla está al final de este documento.

## Comandos de apoyo

Puedes guardarlos como alias en cada EC2.

```bash
# EC2 C: estado de la cola (mensajes Ready y Unacked)
docker exec mesatech-rabbitmq rabbitmqctl list_queues -p mesatech name durable messages_ready messages_unacknowledged consumers

# EC2 A: consultar RDS Catálogo (pide la clave de catalogo_app)
mysql -h <endpoint-rds-catalogo> -u catalogo_app -p mesatech_catalogo \
  -e "SELECT @@hostname AS rds, id, nombre FROM categorias ORDER BY id DESC LIMIT 5;"

# EC2 B: consultar RDS Solicitudes
mysql -h <endpoint-rds-solicitudes> -u solicitudes_app -p mesatech_solicitudes \
  -e "SELECT @@hostname AS rds, categoria_id, nombre, event_id, fecha_recepcion FROM categorias_recibidas ORDER BY fecha_recepcion DESC LIMIT 5;"

# Logs: seguir un evento por su eventId
journalctl -u mesatech-catalogo    --since "10 min ago" | grep -E "categoriaId|eventId"    # EC2 A
journalctl -u mesatech-solicitudes --since "10 min ago" | grep -E "categoriaId|eventId"    # EC2 B
```

> **No** uses *Get messages* con *Automatic ack* durante P3 y P5, porque elimina el mensaje de la cola.
> Si necesitas ver el contenido, usa **Ack mode: "Nack message requeue true"** y anótalo en el informe.

---

## P1. Separación real

Ejecuta en **cada** EC2:
```bash
TOKEN=$(curl -sX PUT http://169.254.169.254/latest/api/token -H "X-aws-ec2-metadata-token-ttl-seconds: 300")
echo "Instance ID: $(curl -s -H "X-aws-ec2-metadata-token: $TOKEN" http://169.254.169.254/latest/meta-data/instance-id)"
echo "IP privada:  $(curl -s -H "X-aws-ec2-metadata-token: $TOKEN" http://169.254.169.254/latest/meta-data/local-ipv4)"
hostname
sudo ss -tlnp | grep -E "java|docker"              # proceso y puerto
systemctl status mesatech-* --no-pager | head -20  # en A y B
sudo grep -E "DB_URL|RABBITMQ_HOST|USERNAME" /etc/mesatech/*.env   # endpoint y usuario, SIN mostrar PASSWORD
```
En la consola de RDS, muestra los **dos identificadores y los dos endpoints distintos**. Luego ejecuta `SELECT @@hostname`
en cada RDS desde su EC2.

**Resultado esperado:** la EC2 A y la EC2 B tienen Instance ID distintos. Catálogo se conecta a `mesatech-catalogo-db`
y usa la tabla `categorias`. Solicitudes se conecta a `mesatech-solicitudes-db` y usa la tabla `categorias_recibidas`.

## P2. Flujo completo

1. Inicia sesión en React como ADMINISTRADOR y crea la categoría **`P2-Redes`**.
2. Anota el `id` y el `eventId` del mensaje verde. También aparecen en DevTools → Network, en el header `X-Event-Id`.
3. En la EC2 A, revisa el log: `Categoría guardada ... categoriaId=N`, `Publicando evento ... eventId=...` y `Evento confirmado por RabbitMQ (ack)`.
4. En la EC2 A, ejecuta el `SELECT` en RDS Catálogo y verifica la fila `N`.
5. En la EC2 B, revisa el log: `Evento recibido ... eventId=... categoriaId=N` y `Copia guardada en RDS Solicitudes`.
6. En la EC2 B, ejecuta el `SELECT` en `categorias_recibidas`: la fila `categoria_id=N` tiene el **mismo** `event_id`.
7. En React, la columna "En Solicitudes" cambia a **Disponible**.
8. Inicia sesión como CLIENTE y crea una solicitud con `P2-Redes`. Consulta `solicitudes` en RDS Solicitudes.

**Trazabilidad:** petición HTTP → `categoriaId=N` → `eventId`/`messageId` → log del consumidor → fila con `event_id`.

## P3. Consumidor detenido

```bash
sudo systemctl stop mesatech-solicitudes     # EC2 B
```
1. Crea la categoría **`P3-Impresoras`** desde React. Debe responder **201**, porque RabbitMQ sí confirmó.
2. RDS Catálogo tiene la fila.
3. En la EC2 C, `list_queues` muestra **messages_ready = 1** y consumers = 0. En la interfaz web aparece *Ready: 1*.
4. RDS Solicitudes **todavía no** tiene la fila, y React la muestra como **Pendiente**.

> Para consultar RDS Solicitudes con el servicio detenido, usa el cliente `mysql` desde la EC2 B. El RDS sigue disponible.

## P4. Consumo diferido

```bash
sudo systemctl start mesatech-solicitudes    # EC2 B
journalctl -u mesatech-solicitudes -f        # ver "Evento recibido" de P3-Impresoras
```
1. El log muestra la recepción y el guardado del evento pendiente.
2. `categorias_recibidas` ahora tiene `P3-Impresoras`.
3. `list_queues` muestra **ready = 0 y unacked = 0**.
4. React la muestra como **Disponible**.

La cola vacía **por sí sola** no demuestra nada. La evidencia es la fila en RDS junto con el log.

## P5. Reinicio del broker

```bash
sudo systemctl stop mesatech-solicitudes                 # EC2 B
```
1. Crea la categoría **`P5-Telefonia`** y verifica que `list_queues` muestra ready = 1.
2. Recrea el contenedor **conservando el volumen y el hostname** (EC2 C):
   ```bash
   cd ~/rabbitmq
   docker compose down          # SIN -v: el volumen mesatech_rabbitmq_data se conserva
   docker volume ls | grep mesatech
   docker compose up -d
   docker compose ps            # esperar (healthy)
   docker exec mesatech-rabbitmq rabbitmqctl list_vhosts
   docker exec mesatech-rabbitmq rabbitmqctl list_exchanges -p mesatech name type durable | grep catalogo
   docker exec mesatech-rabbitmq rabbitmqctl list_bindings  -p mesatech
   docker exec mesatech-rabbitmq rabbitmqctl list_queues    -p mesatech name durable messages_ready
   ```
   El contenedor tiene un ID nuevo (se ve en `docker compose ps`), pero el vhost, el exchange, la cola, el binding y **el mensaje (ready = 1)** siguen ahí.
3. Inicia el consumidor (`sudo systemctl start mesatech-solicitudes`) y verifica la fila `P5-Telefonia` en RDS Solicitudes.

**Por qué funciona:** la cola es *durable*, el mensaje es *persistente* (`deliveryMode=2`), los datos están en un
volumen con nombre y el hostname es fijo (`mesatech-rabbitmq`), por lo que el nodo `rabbit@mesatech-rabbitmq` encuentra sus datos.

## P6. Reinicio de aplicaciones

```bash
sudo systemctl restart mesatech-catalogo mesatech-bff   # EC2 A
sudo systemctl restart mesatech-solicitudes             # EC2 B
```
Vuelve a cargar React: siguen estando las categorías, las solicitudes y las categorías recibidas de P2 a P5.
Repite los `SELECT` en ambos RDS para mostrar las mismas filas.

## P7. Fallo de publicación

```bash
cd ~/rabbitmq && docker compose stop       # EC2 C: broker detenido (no se borra nada)
```
1. Crea la categoría **`P7-Correo`** desde React. Debe aparecer un **mensaje de error controlado**: *"La categoría se guardó
   en Catálogo (id N), pero RabbitMQ no confirmó la publicación del evento..."*. La respuesta HTTP es **503** con `publicado:false`.
2. En el log de Catálogo aparece `Broker no disponible al publicar eventId=... categoriaId=N`.
3. RDS Catálogo **sí tiene** la fila `P7-Correo`. Ese es el resultado real.
4. Restablece el broker con `docker compose start` y espera a que esté `healthy`.
5. `P7-Correo` **no** llega a Solicitudes y queda como *Pendiente* en React, porque el evento nunca se publicó.
   Esa es la inconsistencia: Catálogo la tiene y Solicitudes no. Además, si el administrador vuelve a enviar el formulario,
   se crea **otra** categoría con un id distinto. Corregirlo exigiría un patrón outbox o volver a publicar, lo que está fuera del alcance.

## P8. Continuidad con la EP1

| Caso | Cómo demostrarlo | Esperado |
|---|---|---|
| Login y consumo autorizado | Iniciar sesión en React y ver "Respuesta del backend" | 200 con los roles |
| Sin token | `curl -i https://<api-gw>/v2/usuario` | **401** |
| Permiso insuficiente | Con un usuario CLIENTE: DevTools → copiar el token → `curl -i -X POST -H "Authorization: Bearer <token>" -H "Content-Type: application/json" -d '{"nombre":"x","descripcion":"x"}' https://<api-gw>/v2/catalogo` | **403** |
| Regla de estados | Como OPERADOR, intentar pasar una solicitud de CREADA a RESUELTA (por ejemplo con `curl -X PATCH ... -d '{"estado":"RESUELTA"}'`) | **409**: "No se puede cambiar el estado de CREADA a RESUELTA" |
| v1 y v2 | Llamar una ruta v1 y una v2 de la EP1 | Ambas responden |
| Categoría recibida utilizable | P2, paso 8 | Solicitud creada con `categoriaNombre` tomado de la copia local |

---

## Guion de demo en 15 minutos

| Minuto | Contenido | Quién |
|---|---|---|
| 0–2 | Diagrama: React, Entra ID, API Gateway, BFF, EC2 A/B/C, 2 RDS; quién publica, quién consume y quién es dueño de cada base | Integrante 1 |
| 2–4 | P1: Instance IDs, endpoints RDS y security groups (5672 y 15672 restringidos) | Integrante 1 |
| 4–6 | `compose.yaml` (imagen, hostname, volumen, restart, puertos), `docker compose ps` y recursos del vhost | Integrante 2 |
| 6–10 | P2 en vivo (crear categoría → logs → 2 RDS → crear solicitud) y P3/P4 (detener el consumidor, ver Ready 1, iniciarlo) | Integrante 3 |
| 10–12 | Capturas de P5, P6, P7 y P8 (o repetirlas en vivo si el docente lo pide) | Integrante 2 |
| 12–15 | Preguntas | Todos |

**Checklist 30 minutos antes:**
- [ ] Iniciar el Learner Lab y verificar que los dos RDS estén *Disponibles* y las tres EC2 en *En ejecución*.
- [ ] Actualizar las reglas "Mi IP" (22 y 15672) si cambió tu IP.
- [ ] EC2 C: `docker compose ps` muestra *healthy*.
- [ ] EC2 B y EC2 A: `systemctl status mesatech-*` muestra *active (running)*.
- [ ] RabbitMQ: la pestaña Connections muestra 2 conexiones y la cola tiene Consumers: 1.
- [ ] React conectado, con sesiones de ADMIN, CLIENTE y OPERADOR listas (ventanas de incógnito separadas).
- [ ] Terminales abiertas en A, B y C con `journalctl -f` y los `SELECT` preparados.

---

## Preguntas probables de la defensa (C9)

**¿Por qué dos RDS y no dos esquemas?** Cada microservicio debe ser dueño exclusivo de sus datos: credenciales, endpoint y ciclo de
vida propios. Con dos esquemas en una misma instancia comparten servidor y un fallo o una mala credencial afecta a ambos. La pauta lo limita a 10 de 20 puntos.

**¿Qué diferencia hay entre 5672 y 15672?** 5672 es AMQP: por ahí publican y consumen las aplicaciones, solo por red privada desde los security groups de A y B.
15672 es la interfaz HTTP de administración, abierta solo a nuestra IP /32. Ninguno de los dos usa TLS, por eso no se abren al mundo.

**¿Basta con el mapeo `ports:` de Docker?** No. Docker publica el puerto en la EC2, pero el security group decide quién puede llegar a él. Se necesitan ambos.

**¿Qué es el exchange, la cola y el binding?** El exchange `direct` recibe el mensaje y lo enruta según la routing key. El binding dice
"lo que llegue con `categoria.creada` va a `solicitudes.catalogo.queue`". La cola guarda el mensaje hasta que el consumidor lo confirma.

**¿Qué confirma el publisher confirm?** Que **RabbitMQ aceptó** el mensaje (lo enrutó y, al ser persistente, lo guardó). **No** confirma que Solicitudes lo haya guardado en su RDS.
Eso lo confirma el **ack del consumidor**, que se envía solo después de guardar.

**¿Para qué `mandatory` y los returns?** Si no hubiera binding (por ejemplo, si alguien borra la cola), el broker devolvería el mensaje en vez de descartarlo en silencio.
Lo detectamos y respondemos 503.

**¿Qué pasa si se guarda en Catálogo y falla la publicación?** La categoría queda en RDS Catálogo, la respuesta es 503 e indica que se guardó, y Solicitudes nunca la recibe (P7).
No se revierte automáticamente.

**¿Y si RabbitMQ entrega dos veces el mismo mensaje?** Puede pasar, por ejemplo, si el consumidor guarda y se cae antes del ack. Como la clave de la copia es `categoria_id`,
la segunda entrega sobrescribe la misma fila. Aun así, no garantizamos el procesamiento exactamente una vez.

**¿Por qué `hostname` fijo en compose?** RabbitMQ guarda los datos bajo el nombre de nodo `rabbit@<hostname>`. Si el hostname cambiara al recrear el contenedor,
los datos del volumen no se usarían.

**¿Cola durable es lo mismo que mensaje persistente?** No. *Durable* conserva la **definición** de la cola al reiniciar. *Persistente* (deliveryMode 2) hace que el **mensaje** se escriba a disco.
Para P5 se necesitan ambos, más el volumen.

**¿Qué permisos tiene cada usuario y por qué?** El publicador solo puede escribir en `catalogo.exchange`. El consumidor solo puede leer `solicitudes.catalogo.queue`.
Ninguno puede configurar (crear o borrar recursos) ni tiene tags administrativos. Así, si se filtra una credencial, el daño queda limitado.

---

## Plantilla para el informe

| Prueba | Preparación | Datos usados | Resultado esperado | Resultado obtenido | Evidencia |
|---|---|---|---|---|---|
| P1 | | | | | Fig. |
| P2 | | `P2-Redes`, id=, eventId= | | | Fig. |
| P3 | Consumidor detenido | `P3-Impresoras` | Ready 1, sin fila local | | Fig. |
| P4 | | `P3-Impresoras` | Fila local, Ready 0, Unacked 0 | | Fig. |
| P5 | Consumidor detenido y `down`/`up` sin `-v` | `P5-Telefonia` | Recursos y mensaje se conservan | | Fig. |
| P6 | Reinicio de las 3 apps | Datos de P2–P5 | Sin pérdida | | Fig. |
| P7 | Broker detenido | `P7-Correo` | 503 controlado, fila solo en Catálogo | | Fig. |
| P8 | | Usuarios ADMIN, CLIENTE y OPERADOR | 401, 403, 409, v1 y v2 OK | | Fig. |
