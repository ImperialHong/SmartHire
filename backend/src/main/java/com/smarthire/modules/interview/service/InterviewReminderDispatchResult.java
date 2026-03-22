package com.smarthire.modules.interview.service;

public record InterviewReminderDispatchResult(
    int upcomingReminderCount,
    int startingSoonReminderCount
) {

    public int totalCount() {
        return upcomingReminderCount + startingSoonReminderCount;
    }
}
