package com.smarthire.modules.job.dto.response;

import com.smarthire.modules.job.entity.JobEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record JobResponse(
    Long id,
    Long createdBy,
    String title,
    String description,
    String city,
    String category,
    String employmentType,
    String experienceLevel,
    BigDecimal salaryMin,
    BigDecimal salaryMax,
    String status,
    LocalDateTime applicationDeadline,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

    public static JobResponse fromEntity(JobEntity entity) {
        return new JobResponse(
            entity.getId(),
            entity.getCreatedBy(),
            entity.getTitle(),
            entity.getDescription(),
            entity.getCity(),
            entity.getCategory(),
            entity.getEmploymentType(),
            entity.getExperienceLevel(),
            entity.getSalaryMin(),
            entity.getSalaryMax(),
            entity.getStatus(),
            entity.getApplicationDeadline(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
