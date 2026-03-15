package com.smarthire.modules.statistics.dto.response;

import java.time.LocalDateTime;
import java.util.Map;

public record StatisticsOverviewResponse(
    String scope,
    Long ownerUserId,
    JobStatistics jobs,
    ApplicationStatistics applications,
    InterviewStatistics interviews,
    LocalDateTime generatedAt
) {

    public record JobStatistics(long total, Map<String, Long> byStatus) {
    }

    public record ApplicationStatistics(long total, long withResume, Map<String, Long> byStatus) {
    }

    public record InterviewStatistics(
        long total,
        long upcoming,
        Map<String, Long> byStatus,
        Map<String, Long> byResult
    ) {
    }
}
