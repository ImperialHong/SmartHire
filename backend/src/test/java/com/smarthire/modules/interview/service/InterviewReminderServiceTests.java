package com.smarthire.modules.interview.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smarthire.modules.application.entity.ApplicationEntity;
import com.smarthire.modules.application.mapper.ApplicationMapper;
import com.smarthire.modules.interview.entity.InterviewEntity;
import com.smarthire.modules.interview.mapper.InterviewMapper;
import com.smarthire.modules.job.entity.JobEntity;
import com.smarthire.modules.job.mapper.JobMapper;
import com.smarthire.modules.notification.service.NotificationService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InterviewReminderServiceTests {

    @Mock
    private InterviewMapper interviewMapper;

    @Mock
    private ApplicationMapper applicationMapper;

    @Mock
    private JobMapper jobMapper;

    @Mock
    private NotificationService notificationService;

    private InterviewReminderService interviewReminderService;

    @BeforeEach
    void setUp() {
        interviewReminderService = new InterviewReminderService(
            interviewMapper,
            applicationMapper,
            jobMapper,
            notificationService,
            24,
            60
        );
    }

    @Test
    void dispatchScheduledInterviewRemindersShouldSendUpcomingAndStartingSoonNotifications() {
        InterviewEntity upcomingInterview = new InterviewEntity();
        upcomingInterview.setId(201L);
        upcomingInterview.setApplicationId(101L);
        upcomingInterview.setInterviewAt(LocalDateTime.of(2026, 4, 1, 15, 0));
        upcomingInterview.setLocation("Room B");
        upcomingInterview.setStatus("SCHEDULED");

        InterviewEntity startingSoonInterview = new InterviewEntity();
        startingSoonInterview.setId(202L);
        startingSoonInterview.setApplicationId(102L);
        startingSoonInterview.setInterviewAt(LocalDateTime.of(2026, 4, 1, 9, 30));
        startingSoonInterview.setMeetingLink("https://meet.example.com/202");
        startingSoonInterview.setStatus("SCHEDULED");

        when(interviewMapper.selectList(any()))
            .thenReturn(List.of(upcomingInterview))
            .thenReturn(List.of(startingSoonInterview));
        when(applicationMapper.selectBatchIds(List.of(101L))).thenReturn(List.of(buildApplication(101L, 301L, 401L)));
        when(applicationMapper.selectBatchIds(List.of(102L))).thenReturn(List.of(buildApplication(102L, 302L, 402L)));
        when(jobMapper.selectBatchIds(List.of(301L))).thenReturn(List.of(buildJob(301L, "Backend Engineer")));
        when(jobMapper.selectBatchIds(List.of(302L))).thenReturn(List.of(buildJob(302L, "Platform Engineer")));

        InterviewReminderDispatchResult result = interviewReminderService.dispatchScheduledInterviewReminders();

        assertEquals(1, result.upcomingReminderCount());
        assertEquals(1, result.startingSoonReminderCount());
        verify(notificationService).createNotification(
            401L,
            "INTERVIEW_REMINDER",
            "Interview reminder",
            "Your interview for Backend Engineer is within the next 24 hours at 2026-04-01 15:00. Location: Room B.",
            "INTERVIEW",
            201L
        );
        verify(notificationService).createNotification(
            402L,
            "INTERVIEW_REMINDER",
            "Interview starting soon",
            "Your interview for Platform Engineer starts within the next hour at 2026-04-01 09:30. Meeting link: https://meet.example.com/202.",
            "INTERVIEW",
            202L
        );
    }

    @Test
    void dispatchScheduledInterviewRemindersShouldDoNothingWhenNoInterviewMatches() {
        when(interviewMapper.selectList(any())).thenReturn(List.of()).thenReturn(List.of());

        InterviewReminderDispatchResult result = interviewReminderService.dispatchScheduledInterviewReminders();

        assertEquals(0, result.upcomingReminderCount());
        assertEquals(0, result.startingSoonReminderCount());
        verify(applicationMapper, never()).selectBatchIds(any());
        verify(jobMapper, never()).selectBatchIds(any());
        verify(notificationService, never()).createNotification(any(), any(), any(), any(), any(), any());
    }

    private ApplicationEntity buildApplication(Long applicationId, Long jobId, Long candidateId) {
        ApplicationEntity application = new ApplicationEntity();
        application.setId(applicationId);
        application.setJobId(jobId);
        application.setCandidateId(candidateId);
        return application;
    }

    private JobEntity buildJob(Long jobId, String title) {
        JobEntity job = new JobEntity();
        job.setId(jobId);
        job.setTitle(title);
        return job;
    }
}
