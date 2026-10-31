-- Consultas para las evidencias (P1 a P7).

-- En la EC2 A, contra RDS CATÁLOGO:
--   mysql -h <endpoint-rds-catalogo> -u catalogo_app -p mesatech_catalogo
SELECT @@hostname AS servidor_rds, DATABASE() AS base, CURRENT_USER() AS usuario;
SELECT id, nombre, descripcion FROM categorias ORDER BY id DESC LIMIT 10;

-- En la EC2 B, contra RDS SOLICITUDES:
--   mysql -h <endpoint-rds-solicitudes> -u solicitudes_app -p mesatech_solicitudes
-- SELECT @@hostname AS servidor_rds, DATABASE() AS base, CURRENT_USER() AS usuario;
-- SELECT categoria_id, nombre, event_id, fecha_evento, fecha_recepcion
--   FROM categorias_recibidas ORDER BY fecha_recepcion DESC LIMIT 10;
-- SELECT id, titulo, categoria_id, categoria_nombre, prioridad, estado, usuario, fecha_creacion
--   FROM solicitudes ORDER BY id DESC LIMIT 10;
