package com.smarthire.modules.admin.dto.request;

public record AdminUserListRequest(
    long page,
    long size,
    String keyword,
    String status,
    String roleCode
) {
}
