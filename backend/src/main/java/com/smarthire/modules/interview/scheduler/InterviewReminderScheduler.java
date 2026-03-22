package com.smarthire.modules.interview.scheduler;

import com.smarthire.modules.interview.service.InterviewReminderDispatchResult;
import com.smarthire.modules.interview.service.InterviewReminderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(value = "app.interviews.reminder.enabled", havingValue = "true", matchIfMissing = true)
public class InterviewReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(InterviewReminderScheduler.class);

    private final InterviewReminderService interviewReminderService;

    public InterviewReminderScheduler(InterviewReminderService interviewReminderService) {
        this.interviewReminderService = interviewReminderService;
    }

    @Scheduled(
        cron = "${app.interviews.reminder.cron:0 */10 * * * *}",
        zone = "${app.interviews.reminder.zone:Asia/Shanghai}"
    )
    public void dispatchInterviewReminders() {
        InterviewReminderDispatchResult result = interviewReminderService.dispatchScheduledInterviewReminders();
        if (result.totalCount() > 0) {
            log.info(
                "Dispatched {} interview reminders ({} upcoming, {} starting soon)",
                result.totalCount(),
                result.upcomingReminderCount(),
                result.startingSoonReminderCount()
            );
        }
    }
}
