package com.smarthire.modules.statistics.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.smarthire.modules.application.entity.ApplicationEntity;
import com.smarthire.modules.application.mapper.ApplicationMapper;
import com.smarthire.modules.auth.security.AuthenticatedUser;
import com.smarthire.modules.interview.entity.InterviewEntity;
import com.smarthire.modules.interview.mapper.InterviewMapper;
import com.smarthire.modules.job.entity.JobEntity;
import com.smarthire.modules.job.mapper.JobMapper;
import com.smarthire.modules.statistics.dto.response.StatisticsOverviewResponse;
import com.smarthire.modules.statistics.service.impl.StatisticsServiceImpl;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceImplTests {

    @Mock
    private JobMapper jobMapper;

    @Mock
    private ApplicationMapper applicationMapper;

    @Mock
    private InterviewMapper interviewMapper;

    @InjectMocks
    private StatisticsServiceImpl statisticsService;

    @Test
    void getOverviewShouldAggregateGlobalStatisticsForAdmin() {
        when(jobMapper.selectList(any())).thenReturn(List.of(
            buildJob(1L, 10L, "OPEN"),
            buildJob(2L, 11L, "CLOSED"),
            buildJob(3L, 11L, "OPEN")
        ));
        when(applicationMapper.selectList(any())).thenReturn(List.of(
            buildApplication(100L, 1L, "APPLIED", "resumes/1/resume-a.pdf"),
            buildApplication(101L, 2L, "INTERVIEW", null),
            buildApplication(102L, 3L, "OFFERED", "resumes/2/resume-b.pdf")
        ));
        when(interviewMapper.selectList(any())).thenReturn(List.of(
            buildInterview(200L, 101L, "SCHEDULED", "PENDING", LocalDateTime.now().plusDays(2)),
            buildInterview(201L, 102L, "COMPLETED", "PASSED", LocalDateTime.now().minusDays(1))
        ));

        StatisticsOverviewResponse response = statisticsService.getOverview(new AuthenticatedUser(
            99L,
            "admin@example.com",
            "Admin User",
            List.of("ADMIN")
        ));

        assertEquals("ADMIN", response.scope());
        assertNull(response.ownerUserId());
        assertEquals(3L, response.jobs().total());
        assertEquals(2L, response.jobs().byStatus().get("OPEN"));
        assertEquals(1L, response.jobs().byStatus().get("CLOSED"));
        assertEquals(3L, response.applications().total());
        assertEquals(2L, response.applications().withResume());
        assertEquals(1L, response.applications().byStatus().get("INTERVIEW"));
        assertEquals(2L, response.interviews().total());
        assertEquals(1L, response.interviews().upcoming());
        assertEquals(1L, response.interviews().byStatus().get("SCHEDULED"));
        assertEquals(1L, response.interviews().byResult().get("PASSED"));
    }

    @Test
    void getOverviewShouldOnlyIncludeOwnedJobsForHr() {
        when(jobMapper.selectList(any())).thenReturn(List.of(
            buildJob(11L, 10L, "OPEN"),
            buildJob(12L, 10L, "EXPIRED")
        ));
        when(applicationMapper.selectList(any())).thenReturn(List.of(
            buildApplication(201L, 11L, "REVIEWING", "resumes/10/resume.pdf"),
            buildApplication(202L, 12L, "REJECTED", null)
        ));
        when(interviewMapper.selectList(any())).thenReturn(List.of(
            buildInterview(301L, 201L, "CANCELLED", "FAILED", LocalDateTime.now().minusDays(1))
        ));

        StatisticsOverviewResponse response = statisticsService.getOverview(new AuthenticatedUser(
            10L,
            "hr@example.com",
            "HR User",
            List.of("HR")
        ));

        assertEquals("HR", response.scope());
        assertEquals(10L, response.ownerUserId());
        assertEquals(2L, response.jobs().total());
        assertEquals(1L, response.jobs().byStatus().get("OPEN"));
        assertEquals(1L, response.jobs().byStatus().get("EXPIRED"));
        assertEquals(2L, response.applications().total());
        assertEquals(1L, response.applications().byStatus().get("REVIEWING"));
        assertEquals(1L, response.applications().byStatus().get("REJECTED"));
        assertEquals(1L, response.interviews().total());
        assertEquals(1L, response.interviews().byStatus().get("CANCELLED"));
        assertEquals(1L, response.interviews().byResult().get("FAILED"));
    }

    @Test
    void getOverviewShouldRejectCandidateRole() {
        AuthenticatedUser currentUser = new AuthenticatedUser(
            88L,
            "candidate@example.com",
            "Candidate User",
            List.of("CANDIDATE")
        );

        assertThrows(AccessDeniedException.class, () -> statisticsService.getOverview(currentUser));
        verifyNoInteractions(jobMapper, applicationMapper, interviewMapper);
    }

    private JobEntity buildJob(Long id, Long createdBy, String status) {
        JobEntity job = new JobEntity();
        job.setId(id);
        job.setCreatedBy(createdBy);
        job.setStatus(status);
        return job;
    }

    private ApplicationEntity buildApplication(Long id, Long jobId, String status, String resumeFilePath) {
        ApplicationEntity application = new ApplicationEntity();
        application.setId(id);
        application.setJobId(jobId);
        application.setStatus(status);
        application.setResumeFilePath(resumeFilePath);
        return application;
    }

    private InterviewEntity buildInterview(
        Long id,
        Long applicationId,
        String status,
        String result,
        LocalDateTime interviewAt
    ) {
        InterviewEntity interview = new InterviewEntity();
        interview.setId(id);
        interview.setApplicationId(applicationId);
        interview.setStatus(status);
        interview.setResult(result);
        interview.setInterviewAt(interviewAt);
        return interview;
    }
}
