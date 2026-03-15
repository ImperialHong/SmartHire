package com.smarthire.modules.admin.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smarthire.common.api.PageResponse;
import com.smarthire.modules.admin.dto.request.AdminJobListRequest;
import com.smarthire.modules.admin.dto.response.AdminJobResponse;
import com.smarthire.modules.admin.service.impl.AdminJobServiceImpl;
import com.smarthire.modules.auth.entity.UserEntity;
import com.smarthire.modules.auth.mapper.UserMapper;
import com.smarthire.modules.auth.security.AuthenticatedUser;
import com.smarthire.modules.job.entity.JobEntity;
import com.smarthire.modules.job.mapper.JobMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class AdminJobServiceImplTests {

    @Mock
    private JobMapper jobMapper;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private AdminJobServiceImpl adminJobService;

    @Test
    void listJobsShouldReturnPagedJobsWithOwnerDetails() {
        AuthenticatedUser currentUser = new AuthenticatedUser(99L, "admin@example.com", "Admin", List.of("ADMIN"));

        doAnswer(invocation -> {
            Page<JobEntity> page = invocation.getArgument(0);
            page.setRecords(List.of(
                buildJob(11L, 2L, "Backend Engineer", "OPEN"),
                buildJob(12L, 3L, "Data Analyst", "CLOSED")
            ));
            page.setTotal(2);
            return page;
        }).when(jobMapper).selectPage(ArgumentMatchers.<Page<JobEntity>>any(), any());

        when(userMapper.selectBatchIds(any())).thenReturn(List.of(
            buildOwner(2L, "hr@example.com", "HR User", "ACTIVE"),
            buildOwner(3L, "owner@example.com", "Owner User", "DISABLED")
        ));

        PageResponse<AdminJobResponse> response = adminJobService.listJobs(
            currentUser,
            new AdminJobListRequest(1, 10, null, null, null)
        );

        assertEquals(2L, response.total());
        assertEquals("Backend Engineer", response.records().getFirst().title());
        assertEquals("hr@example.com", response.records().getFirst().ownerEmail());
        assertEquals("DISABLED", response.records().get(1).ownerStatus());
    }

    @Test
    void listJobsShouldReturnEmptyPageWhenOwnerKeywordMatchesNobody() {
        AuthenticatedUser currentUser = new AuthenticatedUser(99L, "admin@example.com", "Admin", List.of("ADMIN"));
        when(userMapper.selectList(any())).thenReturn(List.of());

        PageResponse<AdminJobResponse> response = adminJobService.listJobs(
            currentUser,
            new AdminJobListRequest(1, 10, null, null, "missing")
        );

        assertEquals(0L, response.total());
        assertEquals(0, response.records().size());
    }

    @Test
    void listJobsShouldRejectNonAdminUser() {
        AuthenticatedUser currentUser = new AuthenticatedUser(1L, "hr@example.com", "HR", List.of("HR"));

        assertThrows(
            AccessDeniedException.class,
            () -> adminJobService.listJobs(currentUser, new AdminJobListRequest(1, 10, null, null, null))
        );
        verifyNoInteractions(jobMapper, userMapper);
    }

    private JobEntity buildJob(Long id, Long createdBy, String title, String status) {
        JobEntity job = new JobEntity();
        job.setId(id);
        job.setCreatedBy(createdBy);
        job.setTitle(title);
        job.setDescription("Sample description for " + title);
        job.setCity("Shanghai");
        job.setCategory("Engineering");
        job.setEmploymentType("FULL_TIME");
        job.setExperienceLevel("MID");
        job.setSalaryMin(BigDecimal.valueOf(15000));
        job.setSalaryMax(BigDecimal.valueOf(25000));
        job.setStatus(status);
        job.setApplicationDeadline(LocalDateTime.of(2026, 3, 20, 18, 0));
        job.setCreatedAt(LocalDateTime.of(2026, 3, 1, 10, 0));
        job.setUpdatedAt(LocalDateTime.of(2026, 3, 15, 9, 0));
        return job;
    }

    private UserEntity buildOwner(Long id, String email, String fullName, String status) {
        UserEntity owner = new UserEntity();
        owner.setId(id);
        owner.setEmail(email);
        owner.setFullName(fullName);
        owner.setStatus(status);
        return owner;
    }
}
