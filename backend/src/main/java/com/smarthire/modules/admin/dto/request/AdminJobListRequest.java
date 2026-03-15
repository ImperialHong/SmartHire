package com.smarthire.modules.admin.dto.request;

public record AdminJobListRequest(
    long page,
    long size,
    String keyword,
    String status,
    String ownerKeyword
) {
}
