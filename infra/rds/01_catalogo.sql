-- =====================================================================
-- RDS CATÁLOGO (instancia exclusiva: mesatech-catalogo-db)
-- Ejecutar conectado con el usuario maestro (admin) DESDE LA EC2 A:
--   mysql -h <endpoint-rds-catalogo> -u admin -p < 01_catalogo.sql
-- Reemplazar la contraseña de ejemplo antes de ejecutar (no subir la real).
-- =====================================================================
CREATE DATABASE IF NOT EXISTS mesatech_catalogo
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Usuario de aplicación: solo tiene permisos sobre SU base de datos
CREATE USER IF NOT EXISTS 'catalogo_app'@'%' IDENTIFIED BY 'CAMBIAR_CLAVE_CATALOGO';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES
  ON mesatech_catalogo.* TO 'catalogo_app'@'%';
FLUSH PRIVILEGES;

USE mesatech_catalogo;

-- Categorías originales (fuente de verdad del catálogo)
CREATE TABLE IF NOT EXISTS categorias (
  id          BIGINT        NOT NULL AUTO_INCREMENT,
  nombre      VARCHAR(150)  NOT NULL,
  descripcion VARCHAR(2000) NOT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB;

-- Prioridades (EP1)
CREATE TABLE IF NOT EXISTS prioridades (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  nombre      VARCHAR(255) NOT NULL,
  descripcion VARCHAR(255) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_prioridades_nombre (nombre)
) ENGINE=InnoDB;

-- Datos de prueba de prioridades (las categorías se crean desde React para generar eventos)
INSERT IGNORE INTO prioridades (nombre, descripcion) VALUES
  ('BAJA',  'Puede esperar'),
  ('MEDIA', 'Afecta el trabajo pero tiene alternativa'),
  ('ALTA',  'Detiene el trabajo del usuario');
