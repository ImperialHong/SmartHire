package com.smarthire.modules.notification.dto.request;

public record NotificationListRequest(long page, long size, Boolean isRead) {
}
