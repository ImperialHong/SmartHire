package com.smarthire.modules.admin.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AdminUserStatusUpdateRequest(
    @NotBlank(message = "status must not be blank")
    String status
) {
}
