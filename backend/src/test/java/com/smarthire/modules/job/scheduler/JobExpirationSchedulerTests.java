package com.smarthire.modules.job.scheduler;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smarthire.modules.job.service.JobMaintenanceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JobExpirationSchedulerTests {

    @Mock
    private JobMaintenanceService jobMaintenanceService;

    @InjectMocks
    private JobExpirationScheduler jobExpirationScheduler;

    @Test
    void expireOverdueJobsShouldDelegateToMaintenanceService() {
        when(jobMaintenanceService.expireOverdueJobs()).thenReturn(2);

        jobExpirationScheduler.expireOverdueJobs();

        verify(jobMaintenanceService).expireOverdueJobs();
    }
}
