-- Adds append-only operation logs for admin auditing.

CREATE TABLE operation_logs (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  operator_user_id BIGINT UNSIGNED NULL COMMENT 'Operator user id',
  operator_email VARCHAR(120) NOT NULL COMMENT 'Operator email snapshot',
  operator_name VARCHAR(100) NOT NULL COMMENT 'Operator name snapshot',
  operator_roles VARCHAR(200) NOT NULL COMMENT 'Operator roles snapshot',
  action VARCHAR(64) NOT NULL COMMENT 'Operation action code',
  target_type VARCHAR(64) NOT NULL COMMENT 'Target entity type',
  target_id BIGINT UNSIGNED NULL COMMENT 'Target entity id',
  details VARCHAR(500) NOT NULL COMMENT 'Human-readable operation summary',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
  PRIMARY KEY (id),
  KEY idx_operation_logs_created_at (created_at),
  KEY idx_operation_logs_operator_user_id (operator_user_id),
  KEY idx_operation_logs_action_target (action, target_type),
  CONSTRAINT fk_operation_logs_operator_user
    FOREIGN KEY (operator_user_id) REFERENCES users (id)
    ON DELETE SET NULL
    ON UPDATE RESTRICT
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Admin-visible operation logs';
