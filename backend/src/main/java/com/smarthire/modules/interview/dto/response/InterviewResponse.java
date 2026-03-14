package com.smarthire.modules.interview.dto.response;

import com.smarthire.modules.application.entity.ApplicationEntity;
import com.smarthire.modules.auth.entity.UserEntity;
import com.smarthire.modules.interview.entity.InterviewEntity;
import com.smarthire.modules.job.entity.JobEntity;
import java.time.LocalDateTime;

public record InterviewResponse(
    Long id,
    Long applicationId,
    Long jobId,
    String jobTitle,
    Long candidateId,
    String candidateName,
    String candidateEmail,
    String applicationStatus,
    Long scheduledBy,
    LocalDateTime interviewAt,
    String location,
    String meetingLink,
    String status,
    String result,
    String remark,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

    public static InterviewResponse from(
        InterviewEntity interview,
        ApplicationEntity application,
        JobEntity job,
        UserEntity candidate
    ) {
        return new InterviewResponse(
            interview.getId(),
            interview.getApplicationId(),
            application == null ? null : application.getJobId(),
            job == null ? null : job.getTitle(),
            application == null ? null : application.getCandidateId(),
            candidate == null ? null : candidate.getFullName(),
            candidate == null ? null : candidate.getEmail(),
            application == null ? null : application.getStatus(),
            interview.getScheduledBy(),
            interview.getInterviewAt(),
            interview.getLocation(),
            interview.getMeetingLink(),
            interview.getStatus(),
            interview.getResult(),
            interview.getRemark(),
            interview.getCreatedAt(),
            interview.getUpdatedAt()
        );
    }
}
