package com.smarthire.modules.job.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.smarthire.common.cache.CacheNames;
import com.smarthire.modules.auth.security.AuthenticatedUser;
import com.smarthire.modules.job.entity.JobEntity;
import com.smarthire.modules.job.mapper.JobMapper;
import com.smarthire.modules.operationlog.service.OperationLogService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobMaintenanceService {

    private static final AuthenticatedUser SYSTEM_SCHEDULER = new AuthenticatedUser(
        null,
        "system@smarthire.local",
        "System Scheduler",
        List.of("SYSTEM")
    );

    private final JobMapper jobMapper;

    private final OperationLogService operationLogService;

    private final CacheManager cacheManager;

    public JobMaintenanceService(
        JobMapper jobMapper,
        OperationLogService operationLogService,
        CacheManager cacheManager
    ) {
        this.jobMapper = jobMapper;
        this.operationLogService = operationLogService;
        this.cacheManager = cacheManager;
    }

    @Transactional
    public int expireOverdueJobs() {
        LocalDateTime now = LocalDateTime.now();
        List<JobEntity> overdueJobs = jobMapper.selectList(
            Wrappers.<JobEntity>lambdaQuery()
                .eq(JobEntity::getStatus, "OPEN")
                .isNotNull(JobEntity::getApplicationDeadline)
                .lt(JobEntity::getApplicationDeadline, now)
        );

        if (overdueJobs.isEmpty()) {
            return 0;
        }

        for (JobEntity currentJob : overdueJobs) {
            currentJob.setStatus("EXPIRED");
            currentJob.setUpdatedAt(now);
            jobMapper.updateById(currentJob);
            operationLogService.record(
                SYSTEM_SCHEDULER,
                "JOB_EXPIRED",
                "JOB",
                currentJob.getId(),
                "Expired job " + currentJob.getTitle() + " after application deadline passed"
            );
        }

        clearJobCaches();
        return overdueJobs.size();
    }

    private void clearJobCaches() {
        clearCache(CacheNames.JOB_DETAILS);
        clearCache(CacheNames.JOB_LISTINGS);
        clearCache(CacheNames.STATISTICS_OVERVIEW);
    }

    private void clearCache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }
}
