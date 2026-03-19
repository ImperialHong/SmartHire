-- Adds an idempotency key for RabbitMQ-driven notification persistence.

ALTER TABLE notifications
  ADD COLUMN event_key VARCHAR(64) NULL COMMENT 'Idempotency key for async notification events' AFTER content;

ALTER TABLE notifications
  ADD CONSTRAINT uk_notifications_event_key UNIQUE (event_key);
