package com.smarthire.modules.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smarthire.common.exception.BusinessException;
import com.smarthire.modules.application.dto.request.ApplicationCreateRequest;
import com.smarthire.modules.application.dto.request.ApplicationStatusUpdateRequest;
import com.smarthire.modules.application.dto.response.ApplicationResponse;
import com.smarthire.modules.application.entity.ApplicationEntity;
import com.smarthire.modules.application.mapper.ApplicationMapper;
import com.smarthire.modules.application.service.impl.ApplicationServiceImpl;
import com.smarthire.modules.auth.mapper.UserMapper;
import com.smarthire.modules.auth.security.AuthenticatedUser;
import com.smarthire.modules.job.entity.JobEntity;
import com.smarthire.modules.job.mapper.JobMapper;
import com.smarthire.modules.notification.service.NotificationService;
import com.smarthire.modules.operationlog.service.OperationLogService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceImplTests {

    @Mock
    private ApplicationMapper applicationMapper;

    @Mock
    private JobMapper jobMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private NotificationService notificationService;

    @Mock
    private OperationLogService operationLogService;

    @InjectMocks
    private ApplicationServiceImpl applicationService;

    @Test
    void applyShouldCreateApplicationWithDefaultStatusAndTimestamps() {
        AuthenticatedUser currentUser = new AuthenticatedUser(
            10L,
            "candidate@example.com",
            "Candidate User",
            List.of("CANDIDATE")
        );
        ApplicationCreateRequest request = new ApplicationCreateRequest(
            100L,
            "/files/resume.pdf",
            "I am interested in this role"
        );

        JobEntity job = buildOpenJob(100L, 99L);
        when(jobMapper.selectById(100L)).thenReturn(job);
        when(applicationMapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            ApplicationEntity application = invocation.getArgument(0);
            application.setId(200L);
            return 1;
        }).when(applicationMapper).insert(any(ApplicationEntity.class));

        ApplicationResponse response = applicationService.apply(currentUser, request);

        assertEquals(200L, response.id());
        assertEquals(100L, response.jobId());
        assertEquals(10L, response.candidateId());
        assertEquals("APPLIED", response.status());
        assertEquals("Backend Engineer", response.jobTitle());
        assertNotNull(response.appliedAt());
        assertNotNull(response.createdAt());
        assertNotNull(response.updatedAt());
        verify(notificationService).createNotification(
            99L,
            "APPLICATION_SUBMITTED",
            "New application received",
            "Candidate User applied for Backend Engineer",
            "APPLICATION",
            200L
        );
    }

    @Test
    void applyShouldRejectDuplicateApplication() {
        AuthenticatedUser currentUser = new AuthenticatedUser(
            10L,
            "candidate@example.com",
            "Candidate User",
            List.of("CANDIDATE")
        );
        ApplicationCreateRequest request = new ApplicationCreateRequest(100L, null, null);

        when(jobMapper.selectById(100L)).thenReturn(buildOpenJob(100L, 99L));
        when(applicationMapper.selectOne(any())).thenReturn(new ApplicationEntity());

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> applicationService.apply(currentUser, request)
        );

        assertEquals("APPLICATION_ALREADY_EXISTS", exception.getCode());
    }

    @Test
    void applyShouldRejectClosedJob() {
        AuthenticatedUser currentUser = new AuthenticatedUser(
            10L,
            "candidate@example.com",
            "Candidate User",
            List.of("CANDIDATE")
        );
        ApplicationCreateRequest request = new ApplicationCreateRequest(100L, null, null);
        JobEntity job = buildOpenJob(100L, 99L);
        job.setStatus("CLOSED");
        when(jobMapper.selectById(100L)).thenReturn(job);

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> applicationService.apply(currentUser, request)
        );

        assertEquals("JOB_NOT_OPEN", exception.getCode());
    }

    @Test
    void updateStatusShouldRejectHrWhoDoesNotOwnTheJob() {
        AuthenticatedUser currentUser = new AuthenticatedUser(
            10L,
            "hr@example.com",
            "HR User",
            List.of("HR")
        );
        ApplicationEntity application = new ApplicationEntity();
        application.setId(200L);
        application.setJobId(100L);
        application.setCandidateId(88L);
        application.setStatus("APPLIED");

        when(applicationMapper.selectById(200L)).thenReturn(application);
        when(jobMapper.selectById(100L)).thenReturn(buildOpenJob(100L, 99L));

        assertThrows(
            AccessDeniedException.class,
            () -> applicationService.updateStatus(
                currentUser,
                200L,
                new ApplicationStatusUpdateRequest("REVIEWING", "Looks promising")
            )
        );
    }

    @Test
    void updateStatusShouldNotifyCandidateWhenStatusChanges() {
        AuthenticatedUser currentUser = new AuthenticatedUser(
            10L,
            "hr@example.com",
            "HR User",
            List.of("HR")
        );
        ApplicationEntity application = new ApplicationEntity();
        application.setId(200L);
        application.setJobId(100L);
        application.setCandidateId(88L);
        application.setStatus("APPLIED");

        when(applicationMapper.selectById(200L)).thenReturn(application);
        when(jobMapper.selectById(100L)).thenReturn(buildOpenJob(100L, 10L));

        applicationService.updateStatus(
            currentUser,
            200L,
            new ApplicationStatusUpdateRequest("REVIEWING", "Looks promising")
        );

        verify(notificationService).createNotification(
            88L,
            "APPLICATION_STATUS_CHANGED",
            "Application status updated",
            "Your application for Backend Engineer is now REVIEWING",
            "APPLICATION",
            200L
        );
    }

    private JobEntity buildOpenJob(Long jobId, Long createdBy) {
        JobEntity job = new JobEntity();
        job.setId(jobId);
        job.setCreatedBy(createdBy);
        job.setTitle("Backend Engineer");
        job.setCity("Shanghai");
        job.setCategory("Engineering");
        job.setStatus("OPEN");
        job.setApplicationDeadline(LocalDateTime.now().plusDays(7));
        return job;
    }
}
