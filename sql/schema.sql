-- ============================================================
-- ONLINE DOCUMENT STORER — MYSQL DATABASE SCHEMA
-- Run this ONCE to set up the database manually (optional;
-- Hibernate auto-creates tables with ddl-auto=update)
-- ============================================================

CREATE DATABASE IF NOT EXISTS document_storer_db
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE document_storer_db;

-- ---- USERS ----
CREATE TABLE IF NOT EXISTS users (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  username       VARCHAR(50)  NOT NULL UNIQUE,
  email          VARCHAR(100) NOT NULL UNIQUE,
  password       VARCHAR(255) NOT NULL,
  first_name     VARCHAR(50),
  last_name      VARCHAR(50),
  profile_picture VARCHAR(500),
  phone_number   VARCHAR(20),
  role           ENUM('USER','ADMIN') NOT NULL DEFAULT 'USER',
  is_active      BOOLEAN DEFAULT TRUE,
  is_verified    BOOLEAN DEFAULT FALSE,
  storage_used   BIGINT DEFAULT 0,
  storage_limit  BIGINT DEFAULT 5368709120,
  last_login     DATETIME,
  dark_mode      BOOLEAN DEFAULT FALSE,
  created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_user_email    (email),
  INDEX idx_user_username (username)
) ENGINE=InnoDB;

-- ---- FOLDERS ----
CREATE TABLE IF NOT EXISTS folders (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  name        VARCHAR(100) NOT NULL,
  description VARCHAR(500),
  color       VARCHAR(7)   DEFAULT '#6366f1',
  owner_id    BIGINT NOT NULL,
  parent_id   BIGINT,
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (owner_id)  REFERENCES users(id)   ON DELETE CASCADE,
  FOREIGN KEY (parent_id) REFERENCES folders(id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- ---- DOCUMENTS ----
CREATE TABLE IF NOT EXISTS documents (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  file_name      VARCHAR(255) NOT NULL,
  original_name  VARCHAR(255) NOT NULL,
  file_type      VARCHAR(50),
  mime_type      VARCHAR(100),
  file_size      BIGINT,
  file_path      VARCHAR(500),
  description    VARCHAR(1000),
  status         ENUM('ACTIVE','ARCHIVED','DELETED','PROCESSING') NOT NULL DEFAULT 'ACTIVE',
  is_encrypted   BOOLEAN DEFAULT FALSE,
  is_shared      BOOLEAN DEFAULT FALSE,
  share_token    VARCHAR(100) UNIQUE,
  share_expiry   DATETIME,
  download_count INT DEFAULT 0,
  view_count     INT DEFAULT 0,
  version        INT DEFAULT 1,
  checksum       VARCHAR(64),
  owner_id       BIGINT NOT NULL,
  folder_id      BIGINT,
  created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (owner_id)  REFERENCES users(id)   ON DELETE CASCADE,
  FOREIGN KEY (folder_id) REFERENCES folders(id) ON DELETE SET NULL,
  INDEX idx_doc_owner  (owner_id),
  INDEX idx_doc_name   (file_name),
  INDEX idx_doc_type   (file_type),
  INDEX idx_doc_folder (folder_id),
  FULLTEXT INDEX ft_doc_search (original_name, description)
) ENGINE=InnoDB;

-- ---- DOCUMENT METADATA ----
CREATE TABLE IF NOT EXISTS document_metadata (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  document_id    BIGINT NOT NULL UNIQUE,
  title          VARCHAR(255),
  author         VARCHAR(100),
  subject        VARCHAR(255),
  keywords       VARCHAR(500),
  page_count     INT,
  word_count     INT,
  language       VARCHAR(20),
  resolution     VARCHAR(20),
  color_mode     VARCHAR(20),
  document_date  DATETIME,
  category       VARCHAR(100),
  notes          TEXT,
  created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---- DOCUMENT VERSIONS ----
CREATE TABLE IF NOT EXISTS document_versions (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  document_id    BIGINT NOT NULL,
  version_number INT NOT NULL,
  file_path      VARCHAR(500) NOT NULL,
  file_size      BIGINT,
  change_notes   VARCHAR(500),
  created_by     BIGINT,
  checksum       VARCHAR(64),
  created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE,
  FOREIGN KEY (created_by)  REFERENCES users(id)     ON DELETE SET NULL
) ENGINE=InnoDB;

-- ---- DOCUMENT TAGS ----
CREATE TABLE IF NOT EXISTS document_tags (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  document_id BIGINT NOT NULL,
  tag_name    VARCHAR(50) NOT NULL,
  tag_color   VARCHAR(7) DEFAULT '#6366f1',
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE,
  INDEX idx_tag_name (tag_name)
) ENGINE=InnoDB;

-- ---- DOCUMENT SHARES ----
CREATE TABLE IF NOT EXISTS document_shares (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  document_id  BIGINT NOT NULL,
  shared_by    BIGINT NOT NULL,
  shared_with  BIGINT,
  shared_email VARCHAR(100),
  permission   ENUM('VIEW','DOWNLOAD','EDIT') DEFAULT 'VIEW',
  expiry_date  DATETIME,
  is_active    BOOLEAN DEFAULT TRUE,
  created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE,
  FOREIGN KEY (shared_by)   REFERENCES users(id)     ON DELETE CASCADE,
  FOREIGN KEY (shared_with) REFERENCES users(id)     ON DELETE SET NULL
) ENGINE=InnoDB;

-- ---- ACTIVITY LOGS ----
CREATE TABLE IF NOT EXISTS activity_logs (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id      BIGINT,
  action       VARCHAR(50) NOT NULL,
  entity_type  VARCHAR(50),
  entity_id    BIGINT,
  entity_name  VARCHAR(255),
  description  VARCHAR(500),
  ip_address   VARCHAR(45),
  user_agent   VARCHAR(500),
  is_success   BOOLEAN DEFAULT TRUE,
  error_message VARCHAR(500),
  created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
  INDEX idx_log_user    (user_id),
  INDEX idx_log_action  (action),
  INDEX idx_log_created (created_at)
) ENGINE=InnoDB;
