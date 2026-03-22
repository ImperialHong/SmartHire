package com.smarthire.modules.interview.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.smarthire.modules.application.entity.ApplicationEntity;
import com.smarthire.modules.application.mapper.ApplicationMapper;
import com.smarthire.modules.interview.entity.InterviewEntity;
import com.smarthire.modules.interview.mapper.InterviewMapper;
import com.smarthire.modules.job.entity.JobEntity;
import com.smarthire.modules.job.mapper.JobMapper;
import com.smarthire.modules.notification.service.NotificationService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class InterviewReminderService {

    private static final String INTERVIEW_STATUS_SCHEDULED = "SCHEDULED";
    private static final DateTimeFormatter INTERVIEW_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final InterviewMapper interviewMapper;

    private final ApplicationMapper applicationMapper;

    private final JobMapper jobMapper;

    private final NotificationService notificationService;

    private final int upcomingWindowHours;

    private final int startingSoonWindowMinutes;

    public InterviewReminderService(
        InterviewMapper interviewMapper,
        ApplicationMapper applicationMapper,
        JobMapper jobMapper,
        NotificationService notificationService,
        @Value("${app.interviews.reminder.upcoming-window-hours:24}") int upcomingWindowHours,
        @Value("${app.interviews.reminder.starting-soon-window-minutes:60}") int startingSoonWindowMinutes
    ) {
        this.interviewMapper = interviewMapper;
        this.applicationMapper = applicationMapper;
        this.jobMapper = jobMapper;
        this.notificationService = notificationService;
        this.upcomingWindowHours = upcomingWindowHours;
        this.startingSoonWindowMinutes = startingSoonWindowMinutes;
    }

    public InterviewReminderDispatchResult dispatchScheduledInterviewReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startingSoonUpperBound = now.plusMinutes(Math.max(startingSoonWindowMinutes, 0));
        LocalDateTime upcomingUpperBound = now.plusHours(Math.max(upcomingWindowHours, 0));

        int upcomingReminderCount = dispatchReminders(
            now.plusMinutes(Math.max(startingSoonWindowMinutes, 0)),
            upcomingUpperBound,
            false
        );
        int startingSoonReminderCount = dispatchReminders(now, startingSoonUpperBound, true);

        return new InterviewReminderDispatchResult(upcomingReminderCount, startingSoonReminderCount);
    }

    private int dispatchReminders(
        LocalDateTime windowStart,
        LocalDateTime windowEnd,
        boolean startingSoon
    ) {
        if (!windowEnd.isAfter(windowStart)) {
            return 0;
        }

        List<InterviewEntity> interviews = interviewMapper.selectList(
            Wrappers.<InterviewEntity>lambdaQuery()
                .eq(InterviewEntity::getStatus, INTERVIEW_STATUS_SCHEDULED)
                .gt(InterviewEntity::getInterviewAt, windowStart)
                .le(InterviewEntity::getInterviewAt, windowEnd)
        );
        if (interviews.isEmpty()) {
            return 0;
        }

        Map<Long, ApplicationEntity> applicationsById = loadApplicationsById(
            interviews.stream().map(InterviewEntity::getApplicationId).distinct().toList()
        );
        Map<Long, JobEntity> jobsById = loadJobsById(
            applicationsById.values().stream().map(ApplicationEntity::getJobId).distinct().toList()
        );

        int dispatchedCount = 0;
        for (InterviewEntity interview : interviews) {
            ApplicationEntity application = applicationsById.get(interview.getApplicationId());
            if (application == null || application.getCandidateId() == null) {
                continue;
            }

            JobEntity job = jobsById.get(application.getJobId());
            String jobTitle = job == null ? "the selected position" : job.getTitle();

            notificationService.createNotification(
                application.getCandidateId(),
                "INTERVIEW_REMINDER",
                startingSoon ? "Interview starting soon" : "Interview reminder",
                buildReminderContent(jobTitle, interview, startingSoon),
                "INTERVIEW",
                interview.getId()
            );
            dispatchedCount++;
        }
        return dispatchedCount;
    }

    private Map<Long, ApplicationEntity> loadApplicationsById(List<Long> applicationIds) {
        if (applicationIds.isEmpty()) {
            return Map.of();
        }

        return applicationMapper.selectBatchIds(applicationIds).stream()
            .collect(Collectors.toMap(ApplicationEntity::getId, Function.identity()));
    }

    private Map<Long, JobEntity> loadJobsById(List<Long> jobIds) {
        if (jobIds.isEmpty()) {
            return Map.of();
        }

        return jobMapper.selectBatchIds(jobIds).stream()
            .collect(Collectors.toMap(JobEntity::getId, Function.identity()));
    }

    private String buildReminderContent(String jobTitle, InterviewEntity interview, boolean startingSoon) {
        StringBuilder builder = new StringBuilder()
            .append("Your interview for ")
            .append(jobTitle)
            .append(startingSoon ? " starts within the next hour at " : " is within the next 24 hours at ")
            .append(INTERVIEW_TIME_FORMATTER.format(interview.getInterviewAt()))
            .append(".");

        if (StringUtils.hasText(interview.getLocation())) {
            builder.append(" Location: ").append(interview.getLocation().trim()).append(".");
        }
        if (StringUtils.hasText(interview.getMeetingLink())) {
            builder.append(" Meeting link: ").append(interview.getMeetingLink().trim()).append(".");
        }

        return builder.toString();
    }
}
