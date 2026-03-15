package com.smarthire.modules.statistics.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.smarthire.modules.application.entity.ApplicationEntity;
import com.smarthire.modules.application.mapper.ApplicationMapper;
import com.smarthire.modules.auth.security.AuthenticatedUser;
import com.smarthire.modules.interview.entity.InterviewEntity;
import com.smarthire.modules.interview.mapper.InterviewMapper;
import com.smarthire.modules.job.entity.JobEntity;
import com.smarthire.modules.job.mapper.JobMapper;
import com.smarthire.modules.statistics.dto.response.StatisticsOverviewResponse;
import com.smarthire.modules.statistics.service.StatisticsService;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class StatisticsServiceImpl implements StatisticsService {

    private static final List<String> JOB_STATUSES = List.of("OPEN", "CLOSED", "EXPIRED");
    private static final List<String> APPLICATION_STATUSES =
        List.of("APPLIED", "REVIEWING", "INTERVIEW", "OFFERED", "REJECTED");
    private static final List<String> INTERVIEW_STATUSES =
        List.of("SCHEDULED", "COMPLETED", "CANCELLED");
    private static final List<String> INTERVIEW_RESULTS =
        List.of("PENDING", "PASSED", "FAILED");

    private final JobMapper jobMapper;
    private final ApplicationMapper applicationMapper;
    private final InterviewMapper interviewMapper;

    public StatisticsServiceImpl(
        JobMapper jobMapper,
        ApplicationMapper applicationMapper,
        InterviewMapper interviewMapper
    ) {
        this.jobMapper = jobMapper;
        this.applicationMapper = applicationMapper;
        this.interviewMapper = interviewMapper;
    }

    @Override
    public StatisticsOverviewResponse getOverview(AuthenticatedUser currentUser) {
        validateHrOrAdminRole(currentUser);

        boolean isAdmin = currentUser.roles().contains("ADMIN");
        List<JobEntity> jobs = loadJobs(currentUser, isAdmin);
        List<ApplicationEntity> applications = loadApplications(jobs);
        List<InterviewEntity> interviews = loadInterviews(applications);
        LocalDateTime now = LocalDateTime.now();

        return new StatisticsOverviewResponse(
            isAdmin ? "ADMIN" : "HR",
            isAdmin ? null : currentUser.userId(),
            new StatisticsOverviewResponse.JobStatistics(
                jobs.size(),
                countValues(jobs.stream().map(JobEntity::getStatus).toList(), JOB_STATUSES)
            ),
            new StatisticsOverviewResponse.ApplicationStatistics(
                applications.size(),
                applications.stream()
                    .filter(application -> StringUtils.hasText(application.getResumeFilePath()))
                    .count(),
                countValues(
                    applications.stream().map(ApplicationEntity::getStatus).toList(),
                    APPLICATION_STATUSES
                )
            ),
            new StatisticsOverviewResponse.InterviewStatistics(
                interviews.size(),
                interviews.stream()
                    .filter(interview -> "SCHEDULED".equals(interview.getStatus()))
                    .filter(interview -> interview.getInterviewAt() != null)
                    .filter(interview -> !interview.getInterviewAt().isBefore(now))
                    .count(),
                countValues(
                    interviews.stream().map(InterviewEntity::getStatus).toList(),
                    INTERVIEW_STATUSES
                ),
                countValues(
                    interviews.stream().map(InterviewEntity::getResult).toList(),
                    INTERVIEW_RESULTS
                )
            ),
            now
        );
    }

    private List<JobEntity> loadJobs(AuthenticatedUser currentUser, boolean isAdmin) {
        if (isAdmin) {
            return jobMapper.selectList(null);
        }

        return jobMapper.selectList(
            Wrappers.<JobEntity>lambdaQuery()
                .eq(JobEntity::getCreatedBy, currentUser.userId())
        );
    }

    private List<ApplicationEntity> loadApplications(List<JobEntity> jobs) {
        List<Long> jobIds = jobs.stream()
            .map(JobEntity::getId)
            .filter(Objects::nonNull)
            .toList();
        if (jobIds.isEmpty()) {
            return List.of();
        }

        return applicationMapper.selectList(
            Wrappers.<ApplicationEntity>lambdaQuery()
                .in(ApplicationEntity::getJobId, jobIds)
        );
    }

    private List<InterviewEntity> loadInterviews(List<ApplicationEntity> applications) {
        List<Long> applicationIds = applications.stream()
            .map(ApplicationEntity::getId)
            .filter(Objects::nonNull)
            .toList();
        if (applicationIds.isEmpty()) {
            return List.of();
        }

        return interviewMapper.selectList(
            Wrappers.<InterviewEntity>lambdaQuery()
                .in(InterviewEntity::getApplicationId, applicationIds)
        );
    }

    private Map<String, Long> countValues(List<String> values, List<String> knownValues) {
        LinkedHashMap<String, Long> counts = new LinkedHashMap<>();
        knownValues.forEach(value -> counts.put(value, 0L));

        for (String value : values) {
            if (!StringUtils.hasText(value)) {
                continue;
            }
            counts.merge(value, 1L, Long::sum);
        }

        return counts;
    }

    private void validateHrOrAdminRole(AuthenticatedUser currentUser) {
        if (currentUser == null || currentUser.roles() == null) {
            throw new AccessDeniedException("Authentication is required");
        }

        boolean isAdmin = currentUser.roles().contains("ADMIN");
        boolean isHr = currentUser.roles().contains("HR");
        if (!isAdmin && !isHr) {
            throw new AccessDeniedException("Only HR or admin can view statistics");
        }
    }
}
