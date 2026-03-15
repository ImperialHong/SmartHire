package com.smarthire.modules.interview.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smarthire.common.api.PageResponse;
import com.smarthire.common.exception.BusinessException;
import com.smarthire.modules.application.entity.ApplicationEntity;
import com.smarthire.modules.application.mapper.ApplicationMapper;
import com.smarthire.modules.auth.entity.UserEntity;
import com.smarthire.modules.auth.mapper.UserMapper;
import com.smarthire.modules.auth.security.AuthenticatedUser;
import com.smarthire.modules.interview.dto.request.InterviewCreateRequest;
import com.smarthire.modules.interview.dto.request.InterviewListRequest;
import com.smarthire.modules.interview.dto.request.InterviewUpdateRequest;
import com.smarthire.modules.interview.dto.response.InterviewResponse;
import com.smarthire.modules.interview.entity.InterviewEntity;
import com.smarthire.modules.interview.mapper.InterviewMapper;
import com.smarthire.modules.interview.service.InterviewService;
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
public class InterviewServiceImpl implements InterviewService {

    private static final String APPLICATION_STATUS_INTERVIEW = "INTERVIEW";
    private static final String INTERVIEW_STATUS_SCHEDULED = "SCHEDULED";
    private static final String INTERVIEW_RESULT_PENDING = "PENDING";
    private static final Set<String> INTERVIEW_STATUSES =
        Set.of("SCHEDULED", "COMPLETED", "CANCELLED");
    private static final Set<String> INTERVIEW_RESULTS =
        Set.of("PENDING", "PASSED", "FAILED");

    private final InterviewMapper interviewMapper;
    private final ApplicationMapper applicationMapper;
    private final JobMapper jobMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;
    private final OperationLogService operationLogService;

    public InterviewServiceImpl(
        InterviewMapper interviewMapper,
        ApplicationMapper applicationMapper,
        JobMapper jobMapper,
        UserMapper userMapper,
        NotificationService notificationService,
        OperationLogService operationLogService
    ) {
        this.interviewMapper = interviewMapper;
        this.applicationMapper = applicationMapper;
        this.jobMapper = jobMapper;
        this.userMapper = userMapper;
        this.notificationService = notificationService;
        this.operationLogService = operationLogService;
    }

    @Override
    @Transactional
    public InterviewResponse scheduleInterview(AuthenticatedUser currentUser, InterviewCreateRequest request) {
        validateHrOrAdminRole(currentUser);

        ApplicationEntity application = getExistingApplication(request.applicationId());
        JobEntity job = getExistingJob(application.getJobId());
        validateJobManagementAccess(currentUser, job);

        InterviewEntity existingInterview = interviewMapper.selectOne(
            Wrappers.<InterviewEntity>lambdaQuery()
                .eq(InterviewEntity::getApplicationId, request.applicationId())
        );
        if (existingInterview != null) {
            throw new BusinessException(
                "INTERVIEW_ALREADY_EXISTS",
                "An interview has already been scheduled for this application"
            );
        }

        if (request.interviewAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("INVALID_INTERVIEW_TIME", "interviewAt must be in the future");
        }

        LocalDateTime now = LocalDateTime.now();
        InterviewEntity interview = new InterviewEntity();
        interview.setApplicationId(request.applicationId());
        interview.setScheduledBy(currentUser.userId());
        interview.setInterviewAt(request.interviewAt());
        interview.setLocation(trimToNull(request.location()));
        interview.setMeetingLink(trimToNull(request.meetingLink()));
        interview.setStatus(INTERVIEW_STATUS_SCHEDULED);
        interview.setResult(INTERVIEW_RESULT_PENDING);
        interview.setRemark(trimToNull(request.remark()));
        interview.setCreatedAt(now);
        interview.setUpdatedAt(now);
        interviewMapper.insert(interview);

        application.setStatus(APPLICATION_STATUS_INTERVIEW);
        application.setStatusUpdatedAt(now);
        application.setLastUpdatedBy(currentUser.userId());
        application.setUpdatedAt(now);
        applicationMapper.updateById(application);

        UserEntity candidate = userMapper.selectById(application.getCandidateId());
        notificationService.createNotification(
            application.getCandidateId(),
            "INTERVIEW_SCHEDULED",
            "Interview scheduled",
            "Your interview for " + job.getTitle() + " is scheduled at " + request.interviewAt(),
            "INTERVIEW",
            interview.getId()
        );
        operationLogService.record(
            currentUser,
            "INTERVIEW_SCHEDULED",
            "INTERVIEW",
            interview.getId(),
            "Scheduled interview for job " + job.getTitle()
        );
        return InterviewResponse.from(interview, application, job, candidate);
    }

    @Override
    @Transactional
    public InterviewResponse updateInterview(
        AuthenticatedUser currentUser,
        Long interviewId,
        InterviewUpdateRequest request
    ) {
        validateHrOrAdminRole(currentUser);

        InterviewEntity interview = getExistingInterview(interviewId);
        ApplicationEntity application = getExistingApplication(interview.getApplicationId());
        JobEntity job = getExistingJob(application.getJobId());
        validateJobManagementAccess(currentUser, job);

        if (request.interviewAt() != null) {
            interview.setInterviewAt(request.interviewAt());
        }
        if (request.location() != null) {
            interview.setLocation(trimToNull(request.location()));
        }
        if (request.meetingLink() != null) {
            interview.setMeetingLink(trimToNull(request.meetingLink()));
        }
        if (request.status() != null) {
            interview.setStatus(normalizeInterviewStatus(request.status()));
        }
        if (request.result() != null) {
            interview.setResult(normalizeInterviewResult(request.result()));
        }
        if (request.remark() != null) {
            interview.setRemark(trimToNull(request.remark()));
        }
        interview.setUpdatedAt(LocalDateTime.now());
        interviewMapper.updateById(interview);

        UserEntity candidate = userMapper.selectById(application.getCandidateId());
        notificationService.createNotification(
            application.getCandidateId(),
            "INTERVIEW_UPDATED",
            "Interview updated",
            "Your interview for " + job.getTitle() + " has been updated",
            "INTERVIEW",
            interview.getId()
        );
        operationLogService.record(
            currentUser,
            "INTERVIEW_UPDATED",
            "INTERVIEW",
            interview.getId(),
            "Updated interview for job " + job.getTitle()
        );
        return InterviewResponse.from(interview, application, job, candidate);
    }

    @Override
    public PageResponse<InterviewResponse> listMyInterviews(
        AuthenticatedUser currentUser,
        InterviewListRequest request
    ) {
        validateCandidateRole(currentUser);

        List<ApplicationEntity> candidateApplications = applicationMapper.selectList(
            Wrappers.<ApplicationEntity>lambdaQuery()
                .eq(ApplicationEntity::getCandidateId, currentUser.userId())
        );
        if (candidateApplications.isEmpty()) {
            return PageResponse.of(List.of(), request.page(), request.size(), 0);
        }

        Map<Long, ApplicationEntity> applicationsById = candidateApplications.stream()
            .collect(Collectors.toMap(ApplicationEntity::getId, Function.identity()));

        var query = Wrappers.<InterviewEntity>lambdaQuery()
            .in(InterviewEntity::getApplicationId, applicationsById.keySet())
            .orderByAsc(InterviewEntity::getInterviewAt);
        if (StringUtils.hasText(request.status())) {
            query.eq(InterviewEntity::getStatus, normalizeInterviewStatus(request.status()));
        }

        Page<InterviewEntity> pageRequest = new Page<>(request.page(), request.size());
        Page<InterviewEntity> page = interviewMapper.selectPage(pageRequest, query);

        Map<Long, JobEntity> jobsById = loadJobsById(page.getRecords().stream()
            .map(InterviewEntity::getApplicationId)
            .map(applicationsById::get)
            .filter(application -> application != null)
            .map(ApplicationEntity::getJobId)
            .distinct()
            .toList());

        List<InterviewResponse> records = page.getRecords().stream()
            .map(interview -> {
                ApplicationEntity application = applicationsById.get(interview.getApplicationId());
                JobEntity job = application == null ? null : jobsById.get(application.getJobId());
                return InterviewResponse.from(interview, application, job, null);
            })
            .toList();

        return PageResponse.of(records, page.getCurrent(), page.getSize(), page.getTotal());
    }

    @Override
    public PageResponse<InterviewResponse> listJobInterviews(
        AuthenticatedUser currentUser,
        Long jobId,
        InterviewListRequest request
    ) {
        validateHrOrAdminRole(currentUser);

        JobEntity job = getExistingJob(jobId);
        validateJobManagementAccess(currentUser, job);

        List<ApplicationEntity> jobApplications = applicationMapper.selectList(
            Wrappers.<ApplicationEntity>lambdaQuery()
                .eq(ApplicationEntity::getJobId, jobId)
        );
        if (jobApplications.isEmpty()) {
            return PageResponse.of(List.of(), request.page(), request.size(), 0);
        }

        Map<Long, ApplicationEntity> applicationsById = jobApplications.stream()
            .collect(Collectors.toMap(ApplicationEntity::getId, Function.identity()));

        var query = Wrappers.<InterviewEntity>lambdaQuery()
            .in(InterviewEntity::getApplicationId, applicationsById.keySet())
            .orderByAsc(InterviewEntity::getInterviewAt);
        if (StringUtils.hasText(request.status())) {
            query.eq(InterviewEntity::getStatus, normalizeInterviewStatus(request.status()));
        }

        Page<InterviewEntity> pageRequest = new Page<>(request.page(), request.size());
        Page<InterviewEntity> page = interviewMapper.selectPage(pageRequest, query);

        Map<Long, UserEntity> candidatesById = loadUsersById(page.getRecords().stream()
            .map(InterviewEntity::getApplicationId)
            .map(applicationsById::get)
            .filter(application -> application != null)
            .map(ApplicationEntity::getCandidateId)
            .distinct()
            .toList());

        List<InterviewResponse> records = page.getRecords().stream()
            .map(interview -> {
                ApplicationEntity application = applicationsById.get(interview.getApplicationId());
                UserEntity candidate = application == null ? null : candidatesById.get(application.getCandidateId());
                return InterviewResponse.from(interview, application, job, candidate);
            })
            .toList();

        return PageResponse.of(records, page.getCurrent(), page.getSize(), page.getTotal());
    }

    private InterviewEntity getExistingInterview(Long interviewId) {
        InterviewEntity interview = interviewMapper.selectById(interviewId);
        if (interview == null) {
            throw new BusinessException("INTERVIEW_NOT_FOUND", "Interview was not found");
        }
        return interview;
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

    private void validateCandidateRole(AuthenticatedUser currentUser) {
        if (currentUser == null || currentUser.roles() == null || !currentUser.roles().contains("CANDIDATE")) {
            throw new AccessDeniedException("Only candidates can view their interviews");
        }
    }

    private void validateHrOrAdminRole(AuthenticatedUser currentUser) {
        if (currentUser == null || currentUser.roles() == null) {
            throw new AccessDeniedException("Authentication is required");
        }

        boolean isAdmin = currentUser.roles().contains("ADMIN");
        boolean isHr = currentUser.roles().contains("HR");
        if (!isAdmin && !isHr) {
            throw new AccessDeniedException("Only HR or admin can manage interviews");
        }
    }

    private void validateJobManagementAccess(AuthenticatedUser currentUser, JobEntity job) {
        boolean isAdmin = currentUser.roles().contains("ADMIN");
        if (!isAdmin && !currentUser.userId().equals(job.getCreatedBy())) {
            throw new AccessDeniedException("You can only manage interviews for your own jobs");
        }
    }

    private String normalizeInterviewStatus(String status) {
        String normalized = status.trim().toUpperCase();
        if (!INTERVIEW_STATUSES.contains(normalized)) {
            throw new BusinessException("INVALID_INTERVIEW_STATUS", "Invalid interview status");
        }
        return normalized;
    }

    private String normalizeInterviewResult(String result) {
        String normalized = result.trim().toUpperCase();
        if (!INTERVIEW_RESULTS.contains(normalized)) {
            throw new BusinessException("INVALID_INTERVIEW_RESULT", "Invalid interview result");
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
