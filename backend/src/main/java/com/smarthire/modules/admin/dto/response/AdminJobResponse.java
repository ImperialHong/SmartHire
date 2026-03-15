package com.smarthire.modules.admin.dto.response;

import com.smarthire.modules.auth.entity.UserEntity;
import com.smarthire.modules.job.entity.JobEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminJobResponse(
    Long id,
    Long createdBy,
    String ownerName,
    String ownerEmail,
    String ownerStatus,
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

    public static AdminJobResponse fromEntities(JobEntity job, UserEntity owner) {
        return new AdminJobResponse(
            job.getId(),
            job.getCreatedBy(),
            owner != null ? owner.getFullName() : null,
            owner != null ? owner.getEmail() : null,
            owner != null ? owner.getStatus() : null,
            job.getTitle(),
            job.getDescription(),
            job.getCity(),
            job.getCategory(),
            job.getEmploymentType(),
            job.getExperienceLevel(),
            job.getSalaryMin(),
            job.getSalaryMax(),
            job.getStatus(),
            job.getApplicationDeadline(),
            job.getCreatedAt(),
            job.getUpdatedAt()
        );
    }
}
