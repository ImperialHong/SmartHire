package com.smarthire.modules.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ApplicationStatusUpdateRequest(
    @NotBlank(message = "status is required")
    String status,

    @Size(max = 2000, message = "hrNote length must be less than or equal to 2000")
    String hrNote
) {
}
