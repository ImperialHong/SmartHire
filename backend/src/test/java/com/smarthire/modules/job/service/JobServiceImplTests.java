package com.smarthire.modules.job.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smarthire.common.exception.BusinessException;
import com.smarthire.modules.auth.security.AuthenticatedUser;
import com.smarthire.modules.job.dto.request.JobCreateRequest;
import com.smarthire.modules.job.dto.request.JobUpdateRequest;
import com.smarthire.modules.job.dto.response.JobResponse;
import com.smarthire.modules.job.entity.JobEntity;
import com.smarthire.modules.job.mapper.JobMapper;
import com.smarthire.modules.job.service.impl.JobServiceImpl;
import com.smarthire.modules.operationlog.service.OperationLogService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class JobServiceImplTests {

    @Mock
    private JobMapper jobMapper;

    @Mock
    private OperationLogService operationLogService;

    @InjectMocks
    private JobServiceImpl jobService;

    @Test
    void createJobShouldAssignCurrentUserAndDefaultStatus() {
        AuthenticatedUser currentUser = new AuthenticatedUser(10L, "hr@example.com", "HR User", java.util.List.of("HR"));
        JobCreateRequest request = new JobCreateRequest(
            "Backend Engineer",
            "Build hiring platform modules",
            "Shanghai",
            "Engineering",
            null,
            "mid",
            new BigDecimal("15000"),
            new BigDecimal("25000"),
            null,
            null
        );

        doAnswer(invocation -> {
            JobEntity job = invocation.getArgument(0);
            job.setId(100L);
            return 1;
        }).when(jobMapper).insert(any(JobEntity.class));

        JobResponse response = jobService.createJob(currentUser, request);

        assertEquals(100L, response.id());
        assertEquals(10L, response.createdBy());
        assertEquals("FULL_TIME", response.employmentType());
        assertEquals("MID", response.experienceLevel());
        assertEquals("OPEN", response.status());
        assertNotNull(response.createdAt());
        assertNotNull(response.updatedAt());
    }

    @Test
    void updateJobShouldRejectNonOwnerHr() {
        AuthenticatedUser currentUser = new AuthenticatedUser(10L, "hr@example.com", "HR User", java.util.List.of("HR"));
        JobEntity existingJob = new JobEntity();
        existingJob.setId(100L);
        existingJob.setCreatedBy(99L);
        when(jobMapper.selectById(100L)).thenReturn(existingJob);

        JobUpdateRequest request = new JobUpdateRequest(
            "Backend Engineer",
            "Updated description",
            "Shanghai",
            "Engineering",
            "FULL_TIME",
            "MID",
            new BigDecimal("15000"),
            new BigDecimal("25000"),
            "OPEN",
            null
        );

        assertThrows(AccessDeniedException.class, () -> jobService.updateJob(currentUser, 100L, request));
    }

    @Test
    void createJobShouldRejectInvalidSalaryRange() {
        AuthenticatedUser currentUser = new AuthenticatedUser(10L, "hr@example.com", "HR User", java.util.List.of("HR"));
        JobCreateRequest request = new JobCreateRequest(
            "Backend Engineer",
            "Build hiring platform modules",
            "Shanghai",
            "Engineering",
            "FULL_TIME",
            "MID",
            new BigDecimal("30000"),
            new BigDecimal("20000"),
            "OPEN",
            null
        );

        BusinessException exception = assertThrows(BusinessException.class, () -> jobService.createJob(currentUser, request));

        assertEquals("INVALID_SALARY_RANGE", exception.getCode());
    }
}
