# Guía de despliegue desde cero en AWS Academy (Learner Lab)

Sigue el orden de esta guía. Cada paso indica **qué captura tomar para el informe** (📸).
Región: **us-east-1 (N. Virginia)**. Ten a mano tu archivo `.pem` (*vockey* o uno propio).

> **Antes de empezar en el Learner Lab**
> - La sesión dura cerca de 4 horas. Al terminar, las EC2 se detienen. Al volver a encenderlas, **cambia la IP pública**, pero **la IP privada se mantiene**.
>   Por eso los servicios se conectan entre sí **por IP privada**, y la EC2 A tiene una **Elastic IP** para que API Gateway apunte siempre a la misma dirección.
> - En el Learner Lab no se pueden crear roles IAM. En RDS hay que **desactivar la supervisión mejorada (Enhanced Monitoring)**; si no, la creación falla.
> - Inicia el laboratorio **30 minutos antes** de presentar y sigue el orden de recuperación del README.

---

## Nombres que usaremos

| Recurso | Nombre |
|---|---|
| EC2 A | `mesatech-catalogo` (Catálogo y BFF) |
| EC2 B | `mesatech-solicitudes` |
| EC2 C | `mesatech-rabbitmq` |
| RDS 1 | `mesatech-catalogo-db` |
| RDS 2 | `mesatech-solicitudes-db` |
| SG EC2 A | `sg-mesatech-catalogo` |
| SG EC2 B | `sg-mesatech-solicitudes` |
| SG EC2 C | `sg-mesatech-rabbitmq` |
| SG RDS 1 | `sg-rds-catalogo` |
| SG RDS 2 | `sg-rds-solicitudes` |

---

## Paso 1. Crear los security groups

**EC2 → Red y seguridad → Security groups → Crear grupo de seguridad.** Usa la VPC por defecto.
Primero crea los cinco grupos **sin reglas de entrada**. Después edítalos, porque varias reglas usan como origen otro grupo de seguridad.

"MI IP" es la opción **Mi IP** de la consola, que agrega tu IP pública con máscara `/32`.

| Security group | Tipo | Puerto | Origen | Propósito |
|---|---|---|---|---|
| `sg-mesatech-catalogo` | SSH | 22 | Mi IP /32 | Administración |
| `sg-mesatech-catalogo` | TCP personalizado | 8080 | 0.0.0.0/0 | BFF, invocado por API Gateway. Las IP de API Gateway no son fijas; toda petición exige un JWT válido |
| `sg-mesatech-solicitudes` | SSH | 22 | Mi IP /32 | Administración |
| `sg-mesatech-solicitudes` | TCP personalizado | 8081 | `sg-mesatech-catalogo` | Solo el BFF de la EC2 A llama a Solicitudes |
| `sg-mesatech-rabbitmq` | SSH | 22 | Mi IP /32 | Administración |
| `sg-mesatech-rabbitmq` | TCP personalizado | 5672 | `sg-mesatech-catalogo` | AMQP del publicador, por red privada |
| `sg-mesatech-rabbitmq` | TCP personalizado | 5672 | `sg-mesatech-solicitudes` | AMQP del consumidor, por red privada |
| `sg-mesatech-rabbitmq` | TCP personalizado | 15672 | Mi IP /32 | Interfaz de administración |
| `sg-rds-catalogo` | MySQL/Aurora | 3306 | `sg-mesatech-catalogo` | Solo Catálogo accede a su RDS |
| `sg-rds-solicitudes` | MySQL/Aurora | 3306 | `sg-mesatech-solicitudes` | Solo Solicitudes accede a su RDS |

El puerto 8082 de Catálogo **no se abre**: el BFF lo llama por `localhost` dentro de la misma EC2 A.
**Nunca** uses 0.0.0.0/0 en 3306, 5672 ni 15672. Si cambia tu IP (por ejemplo, porque te conectas desde otra red), actualiza las reglas "Mi IP".

📸 Captura las reglas de entrada de cada uno de los cinco grupos.

---

## Paso 2. Crear las dos instancias RDS

**Aurora and RDS → Bases de datos → Crear base de datos** (repetir dos veces):

| Opción | RDS 1 | RDS 2 |
|---|---|---|
| Método | Creación estándar / Configuración completa | igual |
| Motor | MySQL 8.4 | igual |
| Plantilla | Capa gratuita (o Desarrollo y pruebas) | igual |
| Identificador | `mesatech-catalogo-db` | `mesatech-solicitudes-db` |
| Usuario maestro | `admin` | `admin` |
| Contraseña maestra | (guárdala en un lugar seguro, **no** en el repositorio) | otra distinta |
| Clase | db.t3.micro o db.t4g.micro | igual |
| Almacenamiento | 20 GiB gp2/gp3, **sin** escalado automático | igual |
| Conectividad | **No conectar** a un recurso EC2 (la regla se hizo a mano en el Paso 1) | igual |
| VPC | Default | igual |
| **Acceso público** | **No** | **No** |
| Grupo de seguridad | Elegir **existente**: `sg-rds-catalogo`. Quitar `default` | `sg-rds-solicitudes`. Quitar `default` |
| Supervisión | **Desactivar** Performance Insights y **Enhanced Monitoring** | igual |
| Nombre de base inicial | (vacío, la crea el script) | (vacío) |

La creación tarda entre 5 y 10 minutos. Copia los dos **endpoints** desde *Conectividad y seguridad*.

📸 Captura la lista de bases de datos con los dos identificadores, el endpoint de cada una, la opción *Acceso público: No* y el security group asociado.

---

## Paso 3. Crear las tres EC2

**EC2 → Lanzar instancia** (repetir tres veces):

| Opción | EC2 A | EC2 B | EC2 C |
|---|---|---|---|
| Nombre | `mesatech-catalogo` | `mesatech-solicitudes` | `mesatech-rabbitmq` |
| AMI | Ubuntu Server 24.04 LTS | igual | igual |
| Tipo | **t3.small** (corre 2 JVM: Catálogo y BFF) | t3.small o t3.micro | t3.small (o t3a.medium, como en el laboratorio) |
| Par de claves | vockey o el tuyo | igual | igual |
| Red | VPC default, **asignar IP pública: habilitar** | igual | igual |
| Security group | **Seleccionar existente**: `sg-mesatech-catalogo` | `sg-mesatech-solicitudes` | `sg-mesatech-rabbitmq` |
| Almacenamiento | 20 GiB gp3 | 20 GiB gp3 | 30 GiB gp3 |

**Elastic IP para la EC2 A:** *EC2 → Direcciones IP elásticas → Asignar → Asociar* a `mesatech-catalogo`.

Anota de cada EC2 su **Instance ID**, la **IP privada** y la IP pública.

📸 Captura la lista de instancias con las columnas Instance ID, nombre e IP privada, y la Elastic IP asociada.

---

## Paso 4. EC2 C: instalar Docker y RabbitMQ desde cero

Conéctate a la EC2 C (*Conectar → EC2 Instance Connect*, o `ssh -i clave.pem ubuntu@<IP-publica-C>`).

### 4.1 Docker Engine y el plugin Compose (repositorio oficial)
```bash
# Comprobar que NO hay Docker ni broker previo (evidencia de "desde cero")
docker --version || echo "Docker no instalado"

sudo apt update
sudo apt install -y ca-certificates curl
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc

sudo tee /etc/apt/sources.list.d/docker.sources <<EOF
Types: deb
URIs: https://download.docker.com/linux/ubuntu
Suites: $(. /etc/os-release && echo "${UBUNTU_CODENAME:-$VERSION_CODENAME}")
Components: stable
Architectures: $(dpkg --print-architecture)
Signed-By: /etc/apt/keyrings/docker.asc
EOF

sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo usermod -aG docker ubuntu     # después: salir y volver a entrar para usar docker sin sudo
sudo systemctl enable docker       # Docker arranca al encender la EC2
```
> En el documento del laboratorio, la línea `Suites:` tenía un error de copia (le faltaba `$(. /etc/os-release && echo "$`). Usa la versión de arriba.

📸 Captura la salida de `docker --version` y de `docker compose version`.

### 4.2 Copiar los archivos del broker
Desde **tu PC**, en la carpeta del repositorio:
```bash
scp -i clave.pem infra/rabbitmq/compose.yaml infra/rabbitmq/.env.example infra/rabbitmq/configurar-recursos.sh ubuntu@<IP-publica-C>:~/rabbitmq/
```
También puedes hacerlo desde la EC2 C con `mkdir ~/rabbitmq && cd ~/rabbitmq && nano compose.yaml` y pegar el contenido.

En la EC2 C:
```bash
cd ~/rabbitmq
cp .env.example .env
nano .env            # cambia TODAS las contraseñas
chmod 600 .env
chmod +x configurar-recursos.sh
```

### 4.3 Validar, descargar, arrancar y diagnosticar
```bash
docker compose config --quiet && echo "compose.yaml válido"
docker compose pull
docker compose up -d
docker compose ps                      # debe mostrar (healthy)
docker port mesatech-rabbitmq          # mapeo 5672 y 15672
docker compose logs --tail 40 rabbitmq
docker exec mesatech-rabbitmq rabbitmq-diagnostics check_running
docker exec mesatech-rabbitmq rabbitmq-diagnostics check_port_connectivity
docker exec mesatech-rabbitmq rabbitmq-diagnostics status | head -30
docker volume inspect mesatech_rabbitmq_data
sudo ufw status                        # firewall del SO (por defecto inactive): documentarlo
```
📸 Captura cada comando. **No** muestres `cat .env` ni `docker compose config` sin `--quiet`, porque imprimen las contraseñas.

### 4.4 Crear virtual host, usuarios, permisos, exchange, cola y binding
```bash
./configurar-recursos.sh
```
La salida debe terminar mostrando los permisos, el binding `catalogo.exchange --[categoria.creada]--> solicitudes.catalogo.queue`
y la cola con `durable=True auto_delete=False`.

Abre `http://<IP-publica-C>:15672` (solo responde desde tu IP) e inicia sesión con el usuario administrador del `.env`.

📸 Captura las pestañas **Admin → Virtual Hosts** y **Admin → Users** (los dos usuarios de aplicación sin tags),
la pestaña **Exchanges** con `catalogo.exchange` y sus bindings, y la pestaña **Queues** con `solicitudes.catalogo.queue` (Features: D).

---

## Paso 5. EC2 A y EC2 B: Java y cliente MySQL

En **ambas** EC2:
```bash
sudo apt update
sudo apt install -y openjdk-21-jre-headless mysql-client-core-8.0
java -version
sudo mkdir -p /etc/mesatech
```
En la EC2 A: `sudo mkdir -p /opt/mesatech/catalogo /opt/mesatech/bff && sudo chown -R ubuntu: /opt/mesatech`

En la EC2 B: `sudo mkdir -p /opt/mesatech/solicitudes && sudo chown -R ubuntu: /opt/mesatech`

---

## Paso 6. Crear bases, usuarios y tablas en cada RDS

Los RDS **no tienen acceso público**, así que se configuran **desde la EC2 dueña de cada uno**.

1. Copia los scripts desde tu PC:
   ```bash
   scp -i clave.pem infra/rds/01_catalogo.sql    ubuntu@<IP-publica-A>:~
   scp -i clave.pem infra/rds/02_solicitudes.sql ubuntu@<IP-publica-B>:~
   ```
2. En cada EC2, edita la contraseña de ejemplo del usuario de aplicación (`nano 01_catalogo.sql`) y ejecuta el script:
   ```bash
   # EC2 A
   mysql -h <endpoint-rds-catalogo> -u admin -p < 01_catalogo.sql
   # EC2 B
   mysql -h <endpoint-rds-solicitudes> -u admin -p < 02_solicitudes.sql
   ```
3. Comprueba que el usuario de aplicación puede entrar:
   ```bash
   mysql -h <endpoint-rds-catalogo> -u catalogo_app -p mesatech_catalogo -e "SHOW TABLES; SELECT @@hostname;"
   ```
4. Después de ejecutar los scripts, borra la contraseña del archivo o el archivo completo: `rm 01_catalogo.sql`.

Para ver los datos desde **MySQL Workbench** en tu PC, crea una conexión **Standard TCP/IP over SSH**:
SSH Hostname `<IP-publica-A>:22`, SSH Username `ubuntu`, SSH Key File tu `.pem`,
MySQL Hostname `<endpoint-rds-catalogo>`, puerto 3306. Haz lo mismo con la EC2 B para el otro RDS.
Ese túnel es autorizado porque pasa por la EC2 dueña del RDS.

📸 Captura `SHOW TABLES` en ambos RDS y la conexión de Workbench por túnel SSH.

---

## Paso 7. Compilar y desplegar los microservicios

### 7.1 Compilar en tu PC
```bash
cd backend/microservicio-catalogo    && ./mvnw clean package && cd -
cd backend/microservicio-solicitudes && ./mvnw clean package && cd -
cd backend/backend-springboot-jwt    && ./mvnw clean package && cd -
```

### 7.2 Copiar los .jar y las unidades systemd
```bash
# EC2 A: Catálogo y BFF
scp -i clave.pem backend/microservicio-catalogo/target/microservicio-catalogo-0.0.1-SNAPSHOT.jar ubuntu@<IP-A>:/opt/mesatech/catalogo/app.jar
scp -i clave.pem backend/backend-springboot-jwt/target/api-cloud-native-0.0.1-SNAPSHOT.jar      ubuntu@<IP-A>:/opt/mesatech/bff/app.jar
scp -i clave.pem infra/systemd/mesatech-catalogo.service infra/systemd/mesatech-bff.service \
    backend/microservicio-catalogo/.env.example ubuntu@<IP-A>:~

# EC2 B: Solicitudes
scp -i clave.pem backend/microservicio-solicitudes/target/microservicio-solicitudes-0.0.1-SNAPSHOT.jar ubuntu@<IP-B>:/opt/mesatech/solicitudes/app.jar
scp -i clave.pem infra/systemd/mesatech-solicitudes.service backend/microservicio-solicitudes/.env.example ubuntu@<IP-B>:~
```

### 7.3 Variables de entorno reales (en cada EC2)
**EC2 B** (`/etc/mesatech/solicitudes.env`):
```bash
sudo cp ~/.env.example /etc/mesatech/solicitudes.env
sudo nano /etc/mesatech/solicitudes.env   # endpoint del RDS Solicitudes, claves reales e IP PRIVADA de la EC2 C
sudo chmod 600 /etc/mesatech/solicitudes.env
```
**EC2 A** (`/etc/mesatech/catalogo.env` y `/etc/mesatech/bff.env`):
```bash
sudo cp ~/.env.example /etc/mesatech/catalogo.env
sudo nano /etc/mesatech/catalogo.env      # endpoint del RDS Catálogo, claves reales e IP PRIVADA de la EC2 C
sudo tee /etc/mesatech/bff.env > /dev/null <<EOF
SERVER_PORT=8080
CATALOGO_URL=http://localhost:8082
SOLICITUDES_URL=http://<IP-PRIVADA-B>:8081
CORS_ORIGINS=http://localhost:3000
LOG_SECURITY=INFO
EOF
sudo chmod 600 /etc/mesatech/*.env
```

### 7.4 Instalar y arrancar los servicios
```bash
# EC2 B (arrancarlo primero)
sudo cp ~/mesatech-solicitudes.service /etc/systemd/system/
sudo systemctl daemon-reload && sudo systemctl enable --now mesatech-solicitudes
journalctl -u mesatech-solicitudes -f      # esperar "Started MicroservicioSolicitudesApplication"

# EC2 A
sudo cp ~/mesatech-catalogo.service ~/mesatech-bff.service /etc/systemd/system/
sudo systemctl daemon-reload && sudo systemctl enable --now mesatech-catalogo mesatech-bff
journalctl -u mesatech-catalogo -f
```
Verificación rápida:
```bash
curl -s localhost:8082/actuator/health   # EC2 A -> {"status":"UP"}
curl -s localhost:8081/actuator/health   # EC2 B -> {"status":"UP"}
curl -s localhost:8080/public/hola       # EC2 A -> BFF funcionando
```
En la interfaz de RabbitMQ, la pestaña **Connections** debe mostrar dos conexiones, una de `catalogo_publicador` y otra de `solicitudes_consumidor`, ambas
desde IP **privadas** (172.31.x.x). En **Queues**, la cola debe tener **Consumers: 1**.

📸 Captura `systemctl status` de cada servicio, las Connections y la cola con su consumidor.

---

## Paso 8. API Gateway

Crea una **HTTP API** (o reutiliza la configuración de la EP1) con integraciones **HTTP** que apunten a la
**Elastic IP** de la EC2 A en el puerto 8080:

| Ruta en API Gateway | Integración |
|---|---|
| `GET /v2/usuario` | `http://<EIP>:8080/api/usuario` |
| `GET /v2/catalogo` y `POST /v2/catalogo` | `http://<EIP>:8080/api/catalogo` |
| `PUT /v2/catalogo/{id}` y `DELETE /v2/catalogo/{id}` | `http://<EIP>:8080/api/catalogo/{id}` |
| `GET /v2/catalogo/prioridades` y `POST /v2/catalogo/prioridades` | `http://<EIP>:8080/api/catalogo/prioridades` |
| `PUT /v2/catalogo/prioridades/{id}` y `DELETE /v2/catalogo/prioridades/{id}` | `http://<EIP>:8080/api/catalogo/prioridades/{id}` |
| **`GET /v2/categorias-locales`** (nueva) | `http://<EIP>:8080/api/categorias-locales` |
| `GET /v2/solicitudes/mias` | `http://<EIP>:8080/api/solicitudes` |
| `POST /v2/solicitudes` | `http://<EIP>:8080/api/solicitudes` |
| `GET /v2/solicitudes` | `http://<EIP>:8080/api/solicitudes/todas` |
| `PATCH /v2/solicitudes/{id}/estado` | `http://<EIP>:8080/api/solicitudes/{id}/estado` |
| Rutas `/v1/...` de la EP1 | Igual que en la EP1 |

**CORS** en API Gateway: Allow-Origin `http://localhost:3000`, Allow-Headers `authorization, content-type`,
Allow-Methods `GET, POST, PUT, PATCH, DELETE, OPTIONS`.

Copia la *Invoke URL* en `frontend/dfs-s3-login-1/.env` (`REACT_APP_API_BASE_URL=...`) y ejecuta `npm start`.

📸 Captura la lista de rutas v1 y v2, y la integración de `/v2/categorias-locales`.

---

## Paso 9. Datos de prueba antes de la evaluación

1. Inicia sesión en React como **ADMINISTRADOR** y crea 2 o 3 categorías (por ejemplo `Redes`, `Hardware`, `Accesos`).
   La columna "En Solicitudes" debe pasar de *Pendiente* a **Disponible**.
2. Inicia sesión como **CLIENTE** y crea una solicitud con una de esas categorías.
3. Inicia sesión como **OPERADOR** y lleva la solicitud por CREADA → ASIGNADA → EN_PROCESO → RESUELTA.

Con esto la demo empieza con datos en ambos RDS. Para las pruebas P2 a P7, usa **categorías nuevas** con nombres
fáciles de seguir (`P2-Redes`, `P3-Impresoras`, `P5-Telefonia`, `P7-Correo`).
