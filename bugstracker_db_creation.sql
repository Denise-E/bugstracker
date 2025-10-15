-- ===========================================================
--  Proyecto: Control de Bugs
--  Autora: Denise Eichenblat
--  Materia: Laboratorio I
--  Fecha: Octubre 2025
-- ===========================================================

-- Crear base de datos
CREATE DATABASE IF NOT EXISTS bugtracker CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE bugtracker;

-- =========================================================
-- 1) Catálogo de perfiles
-- =========================================================
CREATE TABLE IF NOT EXISTS perfil_usuario (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  nombre VARCHAR(50) NOT NULL,           -- ADMIN, USUARIO
  PRIMARY KEY (id),
  UNIQUE KEY uq_perfil_usuario_nombre (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Seed perfiles
INSERT INTO perfil_usuario (nombre) VALUES 
  ('ADMIN'),
  ('USUARIO')
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre);

-- =========================================================
-- 2) Usuarios
-- =========================================================
CREATE TABLE IF NOT EXISTS usuario (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  nombre VARCHAR(100) NOT NULL,
  apellido VARCHAR(100) NULL,
  email VARCHAR(255) NOT NULL,           -- sin UNIQUE por alcance (podés agregarlo luego)
  password_hash VARCHAR(255) NOT NULL,   -- hash (ej. Base64 de SHA-256)
  password_salt  VARCHAR(255) NULL,      -- salt si lo usás (Base64)
  perfil_id BIGINT UNSIGNED NOT NULL,
  creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_usuario_perfil (perfil_id),
  CONSTRAINT fk_usuario_perfil
    FOREIGN KEY (perfil_id) REFERENCES perfil_usuario(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================================================
-- 3) Proyectos
-- =========================================================
CREATE TABLE IF NOT EXISTS proyecto (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  nombre VARCHAR(200) NOT NULL,
  descripcion TEXT NULL,
  creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
  -- Podés agregar UNIQUE(nombre) si lo necesitás más adelante
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================================================
-- 4) Catálogo de estados de incidencia
--    (usar exactamente estas etiquetas en tu lógica/UI)
-- =========================================================
CREATE TABLE IF NOT EXISTS incidencia_estado (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  nombre VARCHAR(50) NOT NULL,           -- NUEVA, EN_PROCESO, BLOQUEADA, EN_REVISION, TERMINADA
  PRIMARY KEY (id),
  UNIQUE KEY uq_incidencia_estado_nombre (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Seed estados
INSERT INTO incidencia_estado (nombre) VALUES
  ('NUEVA'),
  ('EN_PROCESO'),
  ('BLOQUEADA'),
  ('EN_REVISION'),
  ('TERMINADA')
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre);

-- =========================================================
-- 5) Incidencias (puntero a versión actual se agrega FK al final)
-- =========================================================
CREATE TABLE IF NOT EXISTS incidencia (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  proyecto_id BIGINT UNSIGNED NOT NULL,
  responsable_id BIGINT UNSIGNED NULL,   -- asignable
  descripcion TEXT NOT NULL,
  estimacion_horas DECIMAL(10,2) NULL,   -- horas estimadas (opcional)
  current_version_id BIGINT UNSIGNED NULL, -- FK circular (se agrega al final)
  creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_incidencia_proyecto (proyecto_id),
  KEY idx_incidencia_responsable (responsable_id),
  KEY idx_incidencia_current_version (current_version_id),
  CONSTRAINT fk_incidencia_proyecto
    FOREIGN KEY (proyecto_id) REFERENCES proyecto(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT fk_incidencia_responsable
    FOREIGN KEY (responsable_id) REFERENCES usuario(id)
    ON UPDATE RESTRICT ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================================================
-- 6) Versiones/Historial de Incidencia (cronología de cambios)
--    Cada registro representa un "hito" (p.ej., cambio de estado).
-- =========================================================
CREATE TABLE IF NOT EXISTS incidencia_version (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  incidencia_id BIGINT UNSIGNED NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by BIGINT UNSIGNED NOT NULL,   -- usuario que hizo el cambio
  estado_id BIGINT UNSIGNED NOT NULL,
  detalles JSON NULL,                    -- JSON (MySQL) para metadatos opcionales
  PRIMARY KEY (id),
  KEY idx_incver_incidencia (incidencia_id),
  KEY idx_incver_created_at (created_at),
  CONSTRAINT fk_incver_incidencia
    FOREIGN KEY (incidencia_id) REFERENCES incidencia(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT fk_incver_created_by
    FOREIGN KEY (created_by) REFERENCES usuario(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT fk_incver_estado
    FOREIGN KEY (estado_id) REFERENCES incidencia_estado(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================================================
-- 7) Comentarios (timeline tipo Linear: mensajes generales)
--    Si en el futuro querés "anclarlos" a una versión, añadís version_id (NULLable).
-- =========================================================
CREATE TABLE IF NOT EXISTS comentario (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  incidencia_id BIGINT UNSIGNED NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by BIGINT UNSIGNED NOT NULL,
  texto TEXT NOT NULL,
  PRIMARY KEY (id),
  KEY idx_comentario_incidencia (incidencia_id),
  KEY idx_comentario_created_at (created_at),
  CONSTRAINT fk_comentario_incidencia
    FOREIGN KEY (incidencia_id) REFERENCES incidencia(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT fk_comentario_created_by
    FOREIGN KEY (created_by) REFERENCES usuario(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================================================
-- 8) Completar la FK circular: incidencia.current_version_id → incidencia_version.id
--    (Se hace al final porque incidencia_version depende de incidencia)
-- =========================================================
ALTER TABLE incidencia
  ADD CONSTRAINT fk_incidencia_current_version
  FOREIGN KEY (current_version_id) REFERENCES incidencia_version(id)
  ON UPDATE RESTRICT ON DELETE SET NULL;