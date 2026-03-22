package com.smarthire.modules.interview.scheduler;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smarthire.modules.interview.service.InterviewReminderDispatchResult;
import com.smarthire.modules.interview.service.InterviewReminderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InterviewReminderSchedulerTests {

    @Mock
    private InterviewReminderService interviewReminderService;

    @InjectMocks
    private InterviewReminderScheduler interviewReminderScheduler;

    @Test
    void dispatchInterviewRemindersShouldDelegateToReminderService() {
        when(interviewReminderService.dispatchScheduledInterviewReminders())
            .thenReturn(new InterviewReminderDispatchResult(1, 1));

        interviewReminderScheduler.dispatchInterviewReminders();

        verify(interviewReminderService).dispatchScheduledInterviewReminders();
    }
}
