package com.smarthire.modules.job.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smarthire.modules.auth.security.AuthenticatedUser;
import com.smarthire.modules.job.entity.JobEntity;
import com.smarthire.modules.job.mapper.JobMapper;
import com.smarthire.modules.operationlog.service.OperationLogService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

@ExtendWith(MockitoExtension.class)
class JobMaintenanceServiceTests {

    @Mock
    private JobMapper jobMapper;

    @Mock
    private OperationLogService operationLogService;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    @InjectMocks
    private JobMaintenanceService jobMaintenanceService;

    @Test
    void expireOverdueJobsShouldExpireMatchingJobsAndClearCaches() {
        JobEntity overdueJob = new JobEntity();
        overdueJob.setId(11L);
        overdueJob.setTitle("Backend Engineer");
        overdueJob.setStatus("OPEN");
        overdueJob.setApplicationDeadline(LocalDateTime.now().minusDays(1));

        when(jobMapper.selectList(any())).thenReturn(List.of(overdueJob));
        when(cacheManager.getCache(any())).thenReturn(cache);

        int expiredCount = jobMaintenanceService.expireOverdueJobs();

        assertEquals(1, expiredCount);
        assertEquals("EXPIRED", overdueJob.getStatus());
        assertNotNull(overdueJob.getUpdatedAt());
        verify(jobMapper).updateById(overdueJob);

        ArgumentCaptor<AuthenticatedUser> userCaptor = ArgumentCaptor.forClass(AuthenticatedUser.class);
        verify(operationLogService).record(
            userCaptor.capture(),
            org.mockito.ArgumentMatchers.eq("JOB_EXPIRED"),
            org.mockito.ArgumentMatchers.eq("JOB"),
            org.mockito.ArgumentMatchers.eq(11L),
            org.mockito.ArgumentMatchers.eq("Expired job Backend Engineer after application deadline passed")
        );
        assertEquals("system@smarthire.local", userCaptor.getValue().email());
        assertEquals("System Scheduler", userCaptor.getValue().fullName());

        verify(cacheManager).getCache("jobDetails");
        verify(cacheManager).getCache("jobListings");
        verify(cacheManager).getCache("statisticsOverview");
        verify(cache, org.mockito.Mockito.times(3)).clear();
    }

    @Test
    void expireOverdueJobsShouldDoNothingWhenNoJobNeedsExpiration() {
        when(jobMapper.selectList(any())).thenReturn(List.of());

        int expiredCount = jobMaintenanceService.expireOverdueJobs();

        assertEquals(0, expiredCount);
        verify(jobMapper, never()).updateById(any(JobEntity.class));
        verify(operationLogService, never()).record(any(), any(), any(), any(), any());
        verify(cacheManager, never()).getCache(any());
    }
}
