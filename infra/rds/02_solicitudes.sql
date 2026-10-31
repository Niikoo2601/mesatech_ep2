-- =====================================================================
-- RDS SOLICITUDES (instancia exclusiva: mesatech-solicitudes-db)
-- Ejecutar conectado con el usuario maestro (admin) DESDE LA EC2 B:
--   mysql -h <endpoint-rds-solicitudes> -u admin -p < 02_solicitudes.sql
-- Reemplazar la contraseña de ejemplo antes de ejecutar (no subir la real).
-- =====================================================================
CREATE DATABASE IF NOT EXISTS mesatech_solicitudes
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'solicitudes_app'@'%' IDENTIFIED BY 'CAMBIAR_CLAVE_SOLICITUDES';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES
  ON mesatech_solicitudes.* TO 'solicitudes_app'@'%';
FLUSH PRIVILEGES;

USE mesatech_solicitudes;

-- Solicitudes de soporte (EP1)
CREATE TABLE IF NOT EXISTS solicitudes (
  id               BIGINT        NOT NULL AUTO_INCREMENT,
  titulo           VARCHAR(150)  NOT NULL,
  descripcion      VARCHAR(2000) NOT NULL,
  categoria_id     BIGINT        NULL,
  categoria_nombre VARCHAR(150)  NULL,
  prioridad        VARCHAR(50)   NOT NULL,
  estado           ENUM('CREADA','ASIGNADA','EN_PROCESO','RESUELTA','CERRADA','CANCELADA') NOT NULL,
  usuario          VARCHAR(150)  NOT NULL,
  fecha_creacion   DATETIME(6)   NOT NULL,
  PRIMARY KEY (id),
  KEY idx_solicitudes_usuario (usuario)
) ENGINE=InnoDB;

-- Copia local de categorías recibidas por RabbitMQ (EP2).
-- categoria_id es el id ORIGINAL de RDS Catálogo (no autogenerado aquí).
CREATE TABLE IF NOT EXISTS categorias_recibidas (
  categoria_id    BIGINT       NOT NULL,
  nombre          VARCHAR(150) NOT NULL,
  event_id        VARCHAR(36)  NOT NULL,
  fecha_evento    DATETIME(6)  NOT NULL,
  fecha_recepcion DATETIME(6)  NOT NULL,
  PRIMARY KEY (categoria_id)
) ENGINE=InnoDB;
