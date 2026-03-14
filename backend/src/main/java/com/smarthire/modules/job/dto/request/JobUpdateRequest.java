package com.smarthire.modules.job.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record JobUpdateRequest(
    @NotBlank(message = "title is required")
    @Size(max = 200, message = "title length must be less than or equal to 200")
    String title,

    @NotBlank(message = "description is required")
    String description,

    @Size(max = 80, message = "city length must be less than or equal to 80")
    String city,

    @Size(max = 80, message = "category length must be less than or equal to 80")
    String category,

    String employmentType,
    String experienceLevel,
    BigDecimal salaryMin,
    BigDecimal salaryMax,
    String status,
    LocalDateTime applicationDeadline
) {
}
