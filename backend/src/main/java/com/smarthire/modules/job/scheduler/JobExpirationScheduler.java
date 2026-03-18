package com.smarthire.modules.job.scheduler;

import com.smarthire.modules.job.service.JobMaintenanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(value = "app.jobs.expiration.enabled", havingValue = "true", matchIfMissing = true)
public class JobExpirationScheduler {

    private static final Logger log = LoggerFactory.getLogger(JobExpirationScheduler.class);

    private final JobMaintenanceService jobMaintenanceService;

    public JobExpirationScheduler(JobMaintenanceService jobMaintenanceService) {
        this.jobMaintenanceService = jobMaintenanceService;
    }

    @Scheduled(
        cron = "${app.jobs.expiration.cron:0 */5 * * * *}",
        zone = "${app.jobs.expiration.zone:Asia/Shanghai}"
    )
    public void expireOverdueJobs() {
        int expiredJobs = jobMaintenanceService.expireOverdueJobs();
        if (expiredJobs > 0) {
            log.info("Expired {} overdue jobs", expiredJobs);
        }
    }
}
