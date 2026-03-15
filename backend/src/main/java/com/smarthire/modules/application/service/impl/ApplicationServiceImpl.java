package com.smarthire.modules.application.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smarthire.common.api.PageResponse;
import com.smarthire.common.exception.BusinessException;
import com.smarthire.modules.application.dto.request.ApplicationCreateRequest;
import com.smarthire.modules.application.dto.request.ApplicationListRequest;
import com.smarthire.modules.application.dto.request.ApplicationStatusUpdateRequest;
import com.smarthire.modules.application.dto.response.ApplicationResponse;
import com.smarthire.modules.application.entity.ApplicationEntity;
import com.smarthire.modules.application.mapper.ApplicationMapper;
import com.smarthire.modules.application.service.ApplicationService;
import com.smarthire.modules.auth.entity.UserEntity;
import com.smarthire.modules.auth.mapper.UserMapper;
import com.smarthire.modules.auth.security.AuthenticatedUser;
import com.smarthire.modules.job.entity.JobEntity;
import com.smarthire.modules.job.mapper.JobMapper;
import com.smarthire.modules.notification.service.NotificationService;
import com.smarthire.modules.operationlog.service.OperationLogService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ApplicationServiceImpl implements ApplicationService {

    private static final String JOB_STATUS_OPEN = "OPEN";
    private static final String APPLICATION_STATUS_APPLIED = "APPLIED";
    private static final Set<String> APPLICATION_STATUSES =
        Set.of("APPLIED", "REVIEWING", "INTERVIEW", "OFFERED", "REJECTED");

    private final ApplicationMapper applicationMapper;
    private final JobMapper jobMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;
    private final OperationLogService operationLogService;

    public ApplicationServiceImpl(
        ApplicationMapper applicationMapper,
        JobMapper jobMapper,
        UserMapper userMapper,
        NotificationService notificationService,
        OperationLogService operationLogService
    ) {
        this.applicationMapper = applicationMapper;
        this.jobMapper = jobMapper;
        this.userMapper = userMapper;
        this.notificationService = notificationService;
        this.operationLogService = operationLogService;
    }

    @Override
    @Transactional
    public ApplicationResponse apply(AuthenticatedUser currentUser, ApplicationCreateRequest request) {
        validateCandidateRole(currentUser);

        JobEntity job = getExistingJob(request.jobId());
        validateJobIsOpen(job);

        ApplicationEntity existingApplication = applicationMapper.selectOne(
            Wrappers.<ApplicationEntity>lambdaQuery()
                .eq(ApplicationEntity::getJobId, request.jobId())
                .eq(ApplicationEntity::getCandidateId, currentUser.userId())
        );
        if (existingApplication != null) {
            throw new BusinessException(
                "APPLICATION_ALREADY_EXISTS",
                "You have already applied for this job"
            );
        }

        LocalDateTime now = LocalDateTime.now();
        ApplicationEntity application = new ApplicationEntity();
        application.setJobId(request.jobId());
        application.setCandidateId(currentUser.userId());
        application.setStatus(APPLICATION_STATUS_APPLIED);
        application.setResumeFilePath(trimToNull(request.resumeFilePath()));
        application.setCoverLetter(trimToNull(request.coverLetter()));
        application.setAppliedAt(now);
        application.setStatusUpdatedAt(now);
        application.setCreatedAt(now);
        application.setUpdatedAt(now);
        applicationMapper.insert(application);

        notificationService.createNotification(
            job.getCreatedBy(),
            "APPLICATION_SUBMITTED",
            "New application received",
            currentUser.fullName() + " applied for " + job.getTitle(),
            "APPLICATION",
            application.getId()
        );
        operationLogService.record(
            currentUser,
            "APPLICATION_SUBMITTED",
            "APPLICATION",
            application.getId(),
            "Submitted application for job " + job.getTitle()
        );

        return ApplicationResponse.forCandidate(application, job);
    }

    @Override
    public PageResponse<ApplicationResponse> listMyApplications(
        AuthenticatedUser currentUser,
        ApplicationListRequest request
    ) {
        validateCandidateRole(currentUser);

        var query = Wrappers.<ApplicationEntity>lambdaQuery()
            .eq(ApplicationEntity::getCandidateId, currentUser.userId())
            .orderByDesc(ApplicationEntity::getAppliedAt);
        if (StringUtils.hasText(request.status())) {
            query.eq(ApplicationEntity::getStatus, normalizeStatus(request.status()));
        }

        Page<ApplicationEntity> pageRequest = new Page<>(request.page(), request.size());
        Page<ApplicationEntity> page = applicationMapper.selectPage(pageRequest, query);

        Map<Long, JobEntity> jobsById = loadJobsById(page.getRecords().stream()
            .map(ApplicationEntity::getJobId)
            .distinct()
            .toList());

        List<ApplicationResponse> records = page.getRecords().stream()
            .map(application -> ApplicationResponse.forCandidate(
                application,
                jobsById.get(application.getJobId())
            ))
            .toList();

        return PageResponse.of(records, page.getCurrent(), page.getSize(), page.getTotal());
    }

    @Override
    public PageResponse<ApplicationResponse> listJobApplications(
        AuthenticatedUser currentUser,
        Long jobId,
        ApplicationListRequest request
    ) {
        validateHrOrAdminRole(currentUser);

        JobEntity job = getExistingJob(jobId);
        validateJobManagementAccess(currentUser, job);

        var query = Wrappers.<ApplicationEntity>lambdaQuery()
            .eq(ApplicationEntity::getJobId, jobId)
            .orderByDesc(ApplicationEntity::getAppliedAt);
        if (StringUtils.hasText(request.status())) {
            query.eq(ApplicationEntity::getStatus, normalizeStatus(request.status()));
        }

        Page<ApplicationEntity> pageRequest = new Page<>(request.page(), request.size());
        Page<ApplicationEntity> page = applicationMapper.selectPage(pageRequest, query);

        Map<Long, UserEntity> candidatesById = loadUsersById(page.getRecords().stream()
            .map(ApplicationEntity::getCandidateId)
            .distinct()
            .toList());

        List<ApplicationResponse> records = page.getRecords().stream()
            .map(application -> ApplicationResponse.forHr(
                application,
                job,
                candidatesById.get(application.getCandidateId())
            ))
            .toList();

        return PageResponse.of(records, page.getCurrent(), page.getSize(), page.getTotal());
    }

    @Override
    @Transactional
    public ApplicationResponse updateStatus(
        AuthenticatedUser currentUser,
        Long applicationId,
        ApplicationStatusUpdateRequest request
    ) {
        validateHrOrAdminRole(currentUser);

        ApplicationEntity application = getExistingApplication(applicationId);
        JobEntity job = getExistingJob(application.getJobId());
        validateJobManagementAccess(currentUser, job);

        LocalDateTime now = LocalDateTime.now();
        String normalizedStatus = normalizeStatus(request.status());
        boolean statusChanged = !normalizedStatus.equals(application.getStatus());
        if (statusChanged) {
            application.setStatus(normalizedStatus);
            application.setStatusUpdatedAt(now);
        }

        application.setHrNote(trimToNull(request.hrNote()));
        application.setLastUpdatedBy(currentUser.userId());
        application.setUpdatedAt(now);
        applicationMapper.updateById(application);

        UserEntity candidate = userMapper.selectById(application.getCandidateId());
        if (statusChanged) {
            notificationService.createNotification(
                application.getCandidateId(),
                "APPLICATION_STATUS_CHANGED",
                "Application status updated",
                "Your application for " + job.getTitle() + " is now " + normalizedStatus,
                "APPLICATION",
                application.getId()
            );
        }
        operationLogService.record(
            currentUser,
            "APPLICATION_STATUS_UPDATED",
            "APPLICATION",
            application.getId(),
            "Updated application status to " + application.getStatus() + " for job " + job.getTitle()
        );
        return ApplicationResponse.forHr(application, job, candidate);
    }

    private ApplicationEntity getExistingApplication(Long applicationId) {
        ApplicationEntity application = applicationMapper.selectById(applicationId);
        if (application == null) {
            throw new BusinessException("APPLICATION_NOT_FOUND", "Application was not found");
        }
        return application;
    }

    private JobEntity getExistingJob(Long jobId) {
        JobEntity job = jobMapper.selectById(jobId);
        if (job == null) {
            throw new BusinessException("JOB_NOT_FOUND", "Job was not found");
        }
        return job;
    }

    private void validateJobIsOpen(JobEntity job) {
        if (!JOB_STATUS_OPEN.equals(job.getStatus())) {
            throw new BusinessException("JOB_NOT_OPEN", "This job is not accepting applications");
        }

        LocalDateTime deadline = job.getApplicationDeadline();
        if (deadline != null && deadline.isBefore(LocalDateTime.now())) {
            throw new BusinessException(
                "APPLICATION_DEADLINE_PASSED",
                "The application deadline has passed"
            );
        }
    }

    private void validateCandidateRole(AuthenticatedUser currentUser) {
        if (currentUser == null || currentUser.roles() == null || !currentUser.roles().contains("CANDIDATE")) {
            throw new AccessDeniedException("Only candidates can apply for jobs");
        }
    }

    private void validateHrOrAdminRole(AuthenticatedUser currentUser) {
        if (currentUser == null || currentUser.roles() == null) {
            throw new AccessDeniedException("Authentication is required");
        }

        boolean isAdmin = currentUser.roles().contains("ADMIN");
        boolean isHr = currentUser.roles().contains("HR");
        if (!isAdmin && !isHr) {
            throw new AccessDeniedException("Only HR or admin can manage applications");
        }
    }

    private void validateJobManagementAccess(AuthenticatedUser currentUser, JobEntity job) {
        boolean isAdmin = currentUser.roles().contains("ADMIN");
        if (!isAdmin && !currentUser.userId().equals(job.getCreatedBy())) {
            throw new AccessDeniedException("You can only manage applications for your own jobs");
        }
    }

    private String normalizeStatus(String status) {
        String normalized = status.trim().toUpperCase();
        if (!APPLICATION_STATUSES.contains(normalized)) {
            throw new BusinessException("INVALID_APPLICATION_STATUS", "Invalid application status");
        }
        return normalized;
    }

    private Map<Long, JobEntity> loadJobsById(List<Long> jobIds) {
        if (jobIds.isEmpty()) {
            return Map.of();
        }

        return jobMapper.selectBatchIds(jobIds).stream()
            .collect(Collectors.toMap(JobEntity::getId, Function.identity()));
    }

    private Map<Long, UserEntity> loadUsersById(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }

        return userMapper.selectBatchIds(userIds).stream()
            .collect(Collectors.toMap(UserEntity::getId, Function.identity()));
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
