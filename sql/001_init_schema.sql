-- SmartHire initial schema / SmartHire 初始数据库结构
-- Target: MySQL 8.0+ / 目标版本：MySQL 8.0+
-- Scope: V2 core workflow (auth, jobs, applications, interviews, notifications)
-- 范围：V2 核心业务流程（认证、岗位、投递、面试、通知）
-- Key assumptions / 关键假设：
-- 1. A candidate can apply to the same job only once. / 同一候选人对同一岗位只能投递一次。
-- 2. The first version supports a single interview record per application. / 第一版每条投递只支持一条面试记录。
-- 3. Roles are fixed to CANDIDATE / HR / ADMIN. / 角色固定为 CANDIDATE / HR / ADMIN。

CREATE DATABASE IF NOT EXISTS smarthire
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE smarthire;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS interviews;
DROP TABLE IF EXISTS applications;
DROP TABLE IF EXISTS user_roles;
DROP TABLE IF EXISTS jobs;
DROP TABLE IF EXISTS roles;
DROP TABLE IF EXISTS users;

SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE users (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  email VARCHAR(120) NOT NULL COMMENT 'Unique login email',
  password_hash VARCHAR(255) NOT NULL COMMENT 'BCrypt or other secure password hash',
  full_name VARCHAR(100) NOT NULL COMMENT 'Display name',
  phone VARCHAR(20) NULL COMMENT 'Optional phone number',
  status ENUM('ACTIVE', 'DISABLED') NOT NULL DEFAULT 'ACTIVE' COMMENT 'User availability status',
  last_login_at DATETIME NULL COMMENT 'Latest successful login time',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update timestamp',
  PRIMARY KEY (id),
  UNIQUE KEY uk_users_email (email),
  KEY idx_users_status (status)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='System users';

CREATE TABLE roles (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  code VARCHAR(32) NOT NULL COMMENT 'Stable role code',
  name VARCHAR(50) NOT NULL COMMENT 'Display name',
  description VARCHAR(255) NULL COMMENT 'Role description',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update timestamp',
  PRIMARY KEY (id),
  UNIQUE KEY uk_roles_code (code)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Fixed system roles';

CREATE TABLE user_roles (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  user_id BIGINT UNSIGNED NOT NULL COMMENT 'Referenced user id',
  role_id BIGINT UNSIGNED NOT NULL COMMENT 'Referenced role id',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_roles_user_role (user_id, role_id),
  KEY idx_user_roles_role_id (role_id),
  CONSTRAINT fk_user_roles_user
    FOREIGN KEY (user_id) REFERENCES users (id)
    ON DELETE CASCADE
    ON UPDATE RESTRICT,
  CONSTRAINT fk_user_roles_role
    FOREIGN KEY (role_id) REFERENCES roles (id)
    ON DELETE CASCADE
    ON UPDATE RESTRICT
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='User to role mapping';

CREATE TABLE jobs (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  created_by BIGINT UNSIGNED NOT NULL COMMENT 'HR user id',
  title VARCHAR(200) NOT NULL COMMENT 'Job title',
  description TEXT NOT NULL COMMENT 'Job description',
  city VARCHAR(80) NULL COMMENT 'Work location city',
  category VARCHAR(80) NULL COMMENT 'Job category',
  employment_type ENUM('FULL_TIME', 'PART_TIME', 'INTERNSHIP', 'CONTRACT') NOT NULL DEFAULT 'FULL_TIME' COMMENT 'Hiring type',
  experience_level ENUM('ENTRY', 'JUNIOR', 'MID', 'SENIOR') NULL COMMENT 'Expected seniority',
  salary_min DECIMAL(10, 2) NULL COMMENT 'Minimum salary',
  salary_max DECIMAL(10, 2) NULL COMMENT 'Maximum salary',
  status ENUM('OPEN', 'CLOSED', 'EXPIRED') NOT NULL DEFAULT 'OPEN' COMMENT 'Job availability status',
  application_deadline DATETIME NULL COMMENT 'Latest application time',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update timestamp',
  PRIMARY KEY (id),
  KEY idx_jobs_created_by (created_by),
  KEY idx_jobs_status_deadline (status, application_deadline),
  KEY idx_jobs_city (city),
  KEY idx_jobs_category (category),
  CONSTRAINT fk_jobs_created_by
    FOREIGN KEY (created_by) REFERENCES users (id)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Published jobs';

CREATE TABLE applications (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  job_id BIGINT UNSIGNED NOT NULL COMMENT 'Applied job id',
  candidate_id BIGINT UNSIGNED NOT NULL COMMENT 'Candidate user id',
  status ENUM('APPLIED', 'REVIEWING', 'INTERVIEW', 'OFFERED', 'REJECTED') NOT NULL DEFAULT 'APPLIED' COMMENT 'Application status',
  resume_file_path VARCHAR(255) NULL COMMENT 'Uploaded resume path',
  cover_letter TEXT NULL COMMENT 'Optional candidate note',
  hr_note TEXT NULL COMMENT 'Optional HR internal note',
  applied_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Initial apply time',
  status_updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Latest status change time',
  last_updated_by BIGINT UNSIGNED NULL COMMENT 'HR or admin who last updated the record',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update timestamp',
  PRIMARY KEY (id),
  UNIQUE KEY uk_applications_job_candidate (job_id, candidate_id),
  KEY idx_applications_candidate_id (candidate_id),
  KEY idx_applications_status (status),
  KEY idx_applications_last_updated_by (last_updated_by),
  CONSTRAINT fk_applications_job
    FOREIGN KEY (job_id) REFERENCES jobs (id)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT,
  CONSTRAINT fk_applications_candidate
    FOREIGN KEY (candidate_id) REFERENCES users (id)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT,
  CONSTRAINT fk_applications_last_updated_by
    FOREIGN KEY (last_updated_by) REFERENCES users (id)
    ON DELETE SET NULL
    ON UPDATE RESTRICT
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Job applications submitted by candidates';

CREATE TABLE interviews (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  application_id BIGINT UNSIGNED NOT NULL COMMENT 'Related application id',
  scheduled_by BIGINT UNSIGNED NULL COMMENT 'HR who scheduled the interview',
  interview_at DATETIME NOT NULL COMMENT 'Scheduled interview time',
  location VARCHAR(255) NULL COMMENT 'Offline location',
  meeting_link VARCHAR(255) NULL COMMENT 'Online meeting link',
  status ENUM('SCHEDULED', 'COMPLETED', 'CANCELLED') NOT NULL DEFAULT 'SCHEDULED' COMMENT 'Interview status',
  result ENUM('PENDING', 'PASSED', 'FAILED') NOT NULL DEFAULT 'PENDING' COMMENT 'Interview outcome',
  remark VARCHAR(500) NULL COMMENT 'Scheduling note or summary',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update timestamp',
  PRIMARY KEY (id),
  UNIQUE KEY uk_interviews_application_id (application_id),
  KEY idx_interviews_scheduled_by (scheduled_by),
  KEY idx_interviews_interview_at (interview_at),
  KEY idx_interviews_status (status),
  CONSTRAINT fk_interviews_application
    FOREIGN KEY (application_id) REFERENCES applications (id)
    ON DELETE CASCADE
    ON UPDATE RESTRICT,
  CONSTRAINT fk_interviews_scheduled_by
    FOREIGN KEY (scheduled_by) REFERENCES users (id)
    ON DELETE SET NULL
    ON UPDATE RESTRICT
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Single-round interview schedule';

CREATE TABLE notifications (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  recipient_user_id BIGINT UNSIGNED NOT NULL COMMENT 'Notification recipient',
  type ENUM(
    'APPLICATION_SUBMITTED',
    'APPLICATION_STATUS_CHANGED',
    'INTERVIEW_SCHEDULED',
    'INTERVIEW_UPDATED',
    'SYSTEM'
  ) NOT NULL COMMENT 'Notification type',
  title VARCHAR(120) NOT NULL COMMENT 'Notification title',
  content VARCHAR(1000) NOT NULL COMMENT 'Notification content',
  related_type VARCHAR(40) NULL COMMENT 'Related entity type',
  related_id BIGINT UNSIGNED NULL COMMENT 'Related entity id',
  is_read TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Read flag',
  read_at DATETIME NULL COMMENT 'Read time',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update timestamp',
  PRIMARY KEY (id),
  KEY idx_notifications_recipient_read_created (recipient_user_id, is_read, created_at),
  KEY idx_notifications_type (type),
  CONSTRAINT fk_notifications_recipient
    FOREIGN KEY (recipient_user_id) REFERENCES users (id)
    ON DELETE CASCADE
    ON UPDATE RESTRICT
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='In-app notifications';

INSERT INTO roles (code, name, description)
VALUES
  ('CANDIDATE', 'Candidate', 'Job seeker role'),
  ('HR', 'HR', 'Recruiter role'),
  ('ADMIN', 'Admin', 'System administrator role')
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  description = VALUES(description),
  updated_at = CURRENT_TIMESTAMP;
