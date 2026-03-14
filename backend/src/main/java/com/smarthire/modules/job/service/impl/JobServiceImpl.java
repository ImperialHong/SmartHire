package com.smarthire.modules.job.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smarthire.common.api.PageResponse;
import com.smarthire.common.exception.BusinessException;
import com.smarthire.modules.auth.security.AuthenticatedUser;
import com.smarthire.modules.job.dto.request.JobCreateRequest;
import com.smarthire.modules.job.dto.request.JobSearchRequest;
import com.smarthire.modules.job.dto.request.JobUpdateRequest;
import com.smarthire.modules.job.dto.response.JobResponse;
import com.smarthire.modules.job.entity.JobEntity;
import com.smarthire.modules.job.mapper.JobMapper;
import com.smarthire.modules.job.service.JobService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class JobServiceImpl implements JobService {

    private static final Set<String> EMPLOYMENT_TYPES =
        Set.of("FULL_TIME", "PART_TIME", "INTERNSHIP", "CONTRACT");
    private static final Set<String> EXPERIENCE_LEVELS =
        Set.of("ENTRY", "JUNIOR", "MID", "SENIOR");
    private static final Set<String> JOB_STATUSES =
        Set.of("OPEN", "CLOSED", "EXPIRED");

    private final JobMapper jobMapper;

    public JobServiceImpl(JobMapper jobMapper) {
        this.jobMapper = jobMapper;
    }

    @Override
    @Transactional
    public JobResponse createJob(AuthenticatedUser currentUser, JobCreateRequest request) {
        validateEditorRole(currentUser);
        validateSalaryRange(request.salaryMin(), request.salaryMax());

        LocalDateTime now = LocalDateTime.now();
        JobEntity job = new JobEntity();
        job.setCreatedBy(currentUser.userId());
        job.setCreatedAt(now);
        job.setUpdatedAt(now);
        applyRequest(job, request);
        jobMapper.insert(job);

        return JobResponse.fromEntity(job);
    }

    @Override
    @Transactional
    public JobResponse updateJob(AuthenticatedUser currentUser, Long jobId, JobUpdateRequest request) {
        validateEditorRole(currentUser);
        validateSalaryRange(request.salaryMin(), request.salaryMax());

        JobEntity existingJob = getExistingJob(jobId);
        validateOwnership(currentUser, existingJob);

        applyRequest(existingJob, request);
        existingJob.setUpdatedAt(LocalDateTime.now());
        jobMapper.updateById(existingJob);

        return JobResponse.fromEntity(existingJob);
    }

    @Override
    @Transactional
    public void deleteJob(AuthenticatedUser currentUser, Long jobId) {
        validateEditorRole(currentUser);

        JobEntity existingJob = getExistingJob(jobId);
        validateOwnership(currentUser, existingJob);

        jobMapper.deleteById(jobId);
    }

    @Override
    public JobResponse getJob(Long jobId) {
        return JobResponse.fromEntity(getExistingJob(jobId));
    }

    @Override
    public PageResponse<JobResponse> listJobs(JobSearchRequest request) {
        LambdaQueryWrapper<JobEntity> query = Wrappers.<JobEntity>lambdaQuery()
            .orderByDesc(JobEntity::getCreatedAt);

        if (StringUtils.hasText(request.keyword())) {
            String keyword = request.keyword().trim();
            query.and(wrapper -> wrapper
                .like(JobEntity::getTitle, keyword)
                .or()
                .like(JobEntity::getDescription, keyword)
            );
        }

        if (StringUtils.hasText(request.city())) {
            query.eq(JobEntity::getCity, request.city().trim());
        }

        if (StringUtils.hasText(request.category())) {
            query.eq(JobEntity::getCategory, request.category().trim());
        }

        if (StringUtils.hasText(request.status())) {
            query.eq(JobEntity::getStatus, normalizeValue(
                request.status(),
                JOB_STATUSES,
                "INVALID_JOB_STATUS",
                "Invalid job status"
            ));
        }

        Page<JobEntity> page = jobMapper.selectPage(new Page<>(request.page(), request.size()), query);
        List<JobResponse> records = page.getRecords().stream()
            .map(JobResponse::fromEntity)
            .toList();

        return PageResponse.of(records, page.getCurrent(), page.getSize(), page.getTotal());
    }

    private JobEntity getExistingJob(Long jobId) {
        JobEntity job = jobMapper.selectById(jobId);
        if (job == null) {
            throw new BusinessException("JOB_NOT_FOUND", "Job was not found");
        }
        return job;
    }

    private void validateEditorRole(AuthenticatedUser currentUser) {
        if (currentUser == null || currentUser.roles() == null) {
            throw new AccessDeniedException("Authentication is required");
        }

        boolean isAdmin = currentUser.roles().contains("ADMIN");
        boolean isHr = currentUser.roles().contains("HR");
        if (!isAdmin && !isHr) {
            throw new AccessDeniedException("Only HR or admin can manage jobs");
        }
    }

    private void validateOwnership(AuthenticatedUser currentUser, JobEntity job) {
        boolean isAdmin = currentUser.roles().contains("ADMIN");
        if (!isAdmin && !currentUser.userId().equals(job.getCreatedBy())) {
            throw new AccessDeniedException("You can only manage jobs created by yourself");
        }
    }

    private void validateSalaryRange(BigDecimal salaryMin, BigDecimal salaryMax) {
        if (salaryMin != null && salaryMax != null && salaryMin.compareTo(salaryMax) > 0) {
            throw new BusinessException("INVALID_SALARY_RANGE", "salaryMin cannot be greater than salaryMax");
        }
    }

    private void applyRequest(JobEntity job, JobCreateRequest request) {
        job.setTitle(request.title().trim());
        job.setDescription(request.description().trim());
        job.setCity(trimToNull(request.city()));
        job.setCategory(trimToNull(request.category()));
        job.setEmploymentType(normalizeValue(
            request.employmentType(),
            EMPLOYMENT_TYPES,
            "INVALID_EMPLOYMENT_TYPE",
            "Invalid employment type",
            "FULL_TIME"
        ));
        job.setExperienceLevel(normalizeNullableValue(
            request.experienceLevel(),
            EXPERIENCE_LEVELS,
            "INVALID_EXPERIENCE_LEVEL",
            "Invalid experience level"
        ));
        job.setSalaryMin(request.salaryMin());
        job.setSalaryMax(request.salaryMax());
        job.setStatus(normalizeValue(
            request.status(),
            JOB_STATUSES,
            "INVALID_JOB_STATUS",
            "Invalid job status",
            "OPEN"
        ));
        job.setApplicationDeadline(request.applicationDeadline());
    }

    private void applyRequest(JobEntity job, JobUpdateRequest request) {
        job.setTitle(request.title().trim());
        job.setDescription(request.description().trim());
        job.setCity(trimToNull(request.city()));
        job.setCategory(trimToNull(request.category()));
        job.setEmploymentType(normalizeValue(
            request.employmentType(),
            EMPLOYMENT_TYPES,
            "INVALID_EMPLOYMENT_TYPE",
            "Invalid employment type",
            "FULL_TIME"
        ));
        job.setExperienceLevel(normalizeNullableValue(
            request.experienceLevel(),
            EXPERIENCE_LEVELS,
            "INVALID_EXPERIENCE_LEVEL",
            "Invalid experience level"
        ));
        job.setSalaryMin(request.salaryMin());
        job.setSalaryMax(request.salaryMax());
        job.setStatus(normalizeValue(
            request.status(),
            JOB_STATUSES,
            "INVALID_JOB_STATUS",
            "Invalid job status",
            "OPEN"
        ));
        job.setApplicationDeadline(request.applicationDeadline());
    }

    private String normalizeValue(
        String rawValue,
        Set<String> allowedValues,
        String errorCode,
        String errorMessage,
        String defaultValue
    ) {
        if (!StringUtils.hasText(rawValue)) {
            return defaultValue;
        }
        return normalizeValue(rawValue, allowedValues, errorCode, errorMessage);
    }

    private String normalizeValue(
        String rawValue,
        Set<String> allowedValues,
        String errorCode,
        String errorMessage
    ) {
        String normalized = rawValue.trim().toUpperCase();
        if (!allowedValues.contains(normalized)) {
            throw new BusinessException(errorCode, errorMessage);
        }
        return normalized;
    }

    private String normalizeNullableValue(
        String rawValue,
        Set<String> allowedValues,
        String errorCode,
        String errorMessage
    ) {
        if (!StringUtils.hasText(rawValue)) {
            return null;
        }
        return normalizeValue(rawValue, allowedValues, errorCode, errorMessage);
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
