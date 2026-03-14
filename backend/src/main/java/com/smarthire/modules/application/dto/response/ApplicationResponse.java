package com.smarthire.modules.application.dto.response;

import com.smarthire.modules.application.entity.ApplicationEntity;
import com.smarthire.modules.auth.entity.UserEntity;
import com.smarthire.modules.job.entity.JobEntity;
import java.time.LocalDateTime;

public record ApplicationResponse(
    Long id,
    Long jobId,
    String jobTitle,
    String jobCity,
    String jobCategory,
    Long candidateId,
    String candidateName,
    String candidateEmail,
    String status,
    String resumeFilePath,
    String coverLetter,
    String hrNote,
    LocalDateTime appliedAt,
    LocalDateTime statusUpdatedAt,
    Long lastUpdatedBy,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

    public static ApplicationResponse forCandidate(ApplicationEntity application, JobEntity job) {
        return new ApplicationResponse(
            application.getId(),
            application.getJobId(),
            job == null ? null : job.getTitle(),
            job == null ? null : job.getCity(),
            job == null ? null : job.getCategory(),
            application.getCandidateId(),
            null,
            null,
            application.getStatus(),
            application.getResumeFilePath(),
            application.getCoverLetter(),
            null,
            application.getAppliedAt(),
            application.getStatusUpdatedAt(),
            null,
            application.getCreatedAt(),
            application.getUpdatedAt()
        );
    }

    public static ApplicationResponse forHr(
        ApplicationEntity application,
        JobEntity job,
        UserEntity candidate
    ) {
        return new ApplicationResponse(
            application.getId(),
            application.getJobId(),
            job == null ? null : job.getTitle(),
            job == null ? null : job.getCity(),
            job == null ? null : job.getCategory(),
            application.getCandidateId(),
            candidate == null ? null : candidate.getFullName(),
            candidate == null ? null : candidate.getEmail(),
            application.getStatus(),
            application.getResumeFilePath(),
            application.getCoverLetter(),
            application.getHrNote(),
            application.getAppliedAt(),
            application.getStatusUpdatedAt(),
            application.getLastUpdatedBy(),
            application.getCreatedAt(),
            application.getUpdatedAt()
        );
    }
}
