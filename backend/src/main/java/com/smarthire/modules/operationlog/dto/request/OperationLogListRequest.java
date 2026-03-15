package com.smarthire.modules.operationlog.dto.request;

public record OperationLogListRequest(
    long page,
    long size,
    String action,
    String targetType,
    Long operatorUserId
) {
}
