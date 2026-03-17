-- Migration: add notification event idempotency key for RabbitMQ-driven delivery
-- 迁移说明：为 RabbitMQ 异步通知增加事件幂等键

ALTER TABLE notifications
  ADD COLUMN event_key VARCHAR(64) NULL COMMENT 'Idempotency key for async notification events' AFTER content;

ALTER TABLE notifications
  ADD CONSTRAINT uk_notifications_event_key UNIQUE (event_key);
