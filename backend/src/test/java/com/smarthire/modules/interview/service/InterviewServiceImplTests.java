package com.smarthire.modules.interview.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smarthire.common.exception.BusinessException;
import com.smarthire.modules.application.entity.ApplicationEntity;
import com.smarthire.modules.application.mapper.ApplicationMapper;
import com.smarthire.modules.auth.entity.UserEntity;
import com.smarthire.modules.auth.mapper.UserMapper;
import com.smarthire.modules.auth.security.AuthenticatedUser;
import com.smarthire.modules.interview.dto.request.InterviewCreateRequest;
import com.smarthire.modules.interview.dto.request.InterviewUpdateRequest;
import com.smarthire.modules.interview.dto.response.InterviewResponse;
import com.smarthire.modules.interview.entity.InterviewEntity;
import com.smarthire.modules.interview.mapper.InterviewMapper;
import com.smarthire.modules.interview.service.impl.InterviewServiceImpl;
import com.smarthire.modules.job.entity.JobEntity;
import com.smarthire.modules.job.mapper.JobMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class InterviewServiceImplTests {

    @Mock
    private InterviewMapper interviewMapper;

    @Mock
    private ApplicationMapper applicationMapper;

    @Mock
    private JobMapper jobMapper;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private InterviewServiceImpl interviewService;

    @Test
    void scheduleInterviewShouldCreateRecordAndUpdateApplicationStatus() {
        AuthenticatedUser currentUser = new AuthenticatedUser(
            10L,
            "hr@example.com",
            "HR User",
            List.of("HR")
        );
        InterviewCreateRequest request = new InterviewCreateRequest(
            200L,
            LocalDateTime.now().plusDays(1),
            "Meeting Room A",
            "https://meet.example.com/123",
            "Please arrive 10 minutes early"
        );

        ApplicationEntity application = buildApplication(200L, 100L, 88L, "REVIEWING");
        JobEntity job = buildJob(100L, 10L);
        UserEntity candidate = buildCandidate(88L);

        when(applicationMapper.selectById(200L)).thenReturn(application);
        when(jobMapper.selectById(100L)).thenReturn(job);
        when(interviewMapper.selectOne(any())).thenReturn(null);
        when(userMapper.selectById(88L)).thenReturn(candidate);
        doAnswer(invocation -> {
            InterviewEntity interview = invocation.getArgument(0);
            interview.setId(300L);
            return 1;
        }).when(interviewMapper).insert(any(InterviewEntity.class));

        InterviewResponse response = interviewService.scheduleInterview(currentUser, request);

        assertEquals(300L, response.id());
        assertEquals(200L, response.applicationId());
        assertEquals(100L, response.jobId());
        assertEquals("Backend Engineer", response.jobTitle());
        assertEquals("INTERVIEW", response.applicationStatus());
        assertEquals("SCHEDULED", response.status());
        assertEquals("PENDING", response.result());
        assertNotNull(response.createdAt());
        verify(applicationMapper).updateById(any(ApplicationEntity.class));
    }

    @Test
    void scheduleInterviewShouldRejectDuplicateInterview() {
        AuthenticatedUser currentUser = new AuthenticatedUser(
            10L,
            "hr@example.com",
            "HR User",
            List.of("HR")
        );
        InterviewCreateRequest request = new InterviewCreateRequest(
            200L,
            LocalDateTime.now().plusDays(1),
            null,
            null,
            null
        );

        when(applicationMapper.selectById(200L)).thenReturn(buildApplication(200L, 100L, 88L, "REVIEWING"));
        when(jobMapper.selectById(100L)).thenReturn(buildJob(100L, 10L));
        when(interviewMapper.selectOne(any())).thenReturn(new InterviewEntity());

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> interviewService.scheduleInterview(currentUser, request)
        );

        assertEquals("INTERVIEW_ALREADY_EXISTS", exception.getCode());
    }

    @Test
    void scheduleInterviewShouldRejectNonOwnerHr() {
        AuthenticatedUser currentUser = new AuthenticatedUser(
            10L,
            "hr@example.com",
            "HR User",
            List.of("HR")
        );
        InterviewCreateRequest request = new InterviewCreateRequest(
            200L,
            LocalDateTime.now().plusDays(1),
            null,
            null,
            null
        );

        when(applicationMapper.selectById(200L)).thenReturn(buildApplication(200L, 100L, 88L, "REVIEWING"));
        when(jobMapper.selectById(100L)).thenReturn(buildJob(100L, 99L));

        assertThrows(
            AccessDeniedException.class,
            () -> interviewService.scheduleInterview(currentUser, request)
        );
    }

    @Test
    void updateInterviewShouldApplyProvidedFields() {
        AuthenticatedUser currentUser = new AuthenticatedUser(
            10L,
            "hr@example.com",
            "HR User",
            List.of("HR")
        );
        InterviewEntity interview = new InterviewEntity();
        interview.setId(300L);
        interview.setApplicationId(200L);
        interview.setStatus("SCHEDULED");
        interview.setResult("PENDING");
        interview.setLocation("Room A");
        interview.setMeetingLink("https://old.example.com");

        when(interviewMapper.selectById(300L)).thenReturn(interview);
        when(applicationMapper.selectById(200L)).thenReturn(buildApplication(200L, 100L, 88L, "INTERVIEW"));
        when(jobMapper.selectById(100L)).thenReturn(buildJob(100L, 10L));
        when(userMapper.selectById(88L)).thenReturn(buildCandidate(88L));

        InterviewResponse response = interviewService.updateInterview(
            currentUser,
            300L,
            new InterviewUpdateRequest(
                LocalDateTime.of(2026, 3, 20, 15, 0),
                "Room B",
                "https://meet.example.com/new",
                "COMPLETED",
                "PASSED",
                "Strong communication"
            )
        );

        assertEquals("COMPLETED", response.status());
        assertEquals("PASSED", response.result());
        assertEquals("Room B", response.location());
        assertEquals("https://meet.example.com/new", response.meetingLink());
    }

    private ApplicationEntity buildApplication(Long applicationId, Long jobId, Long candidateId, String status) {
        ApplicationEntity application = new ApplicationEntity();
        application.setId(applicationId);
        application.setJobId(jobId);
        application.setCandidateId(candidateId);
        application.setStatus(status);
        return application;
    }

    private JobEntity buildJob(Long jobId, Long createdBy) {
        JobEntity job = new JobEntity();
        job.setId(jobId);
        job.setCreatedBy(createdBy);
        job.setTitle("Backend Engineer");
        return job;
    }

    private UserEntity buildCandidate(Long candidateId) {
        UserEntity candidate = new UserEntity();
        candidate.setId(candidateId);
        candidate.setFullName("Candidate User");
        candidate.setEmail("candidate@example.com");
        return candidate;
    }
}
