package com.smarthire.modules.operationlog.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smarthire.common.api.PageResponse;
import com.smarthire.modules.auth.security.AuthenticatedUser;
import com.smarthire.modules.operationlog.dto.request.OperationLogListRequest;
import com.smarthire.modules.operationlog.dto.response.OperationLogResponse;
import com.smarthire.modules.operationlog.entity.OperationLogEntity;
import com.smarthire.modules.operationlog.mapper.OperationLogMapper;
import com.smarthire.modules.operationlog.service.impl.OperationLogServiceImpl;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class OperationLogServiceImplTests {

    @Mock
    private OperationLogMapper operationLogMapper;

    @InjectMocks
    private OperationLogServiceImpl operationLogService;

    @Test
    void recordShouldPersistOperatorSnapshot() {
        AuthenticatedUser currentUser = new AuthenticatedUser(
            99L,
            "admin@example.com",
            "Admin User",
            List.of("ADMIN")
        );

        operationLogService.record(
            currentUser,
            "USER_STATUS_UPDATED",
            "USER",
            2L,
            "Updated user status to DISABLED"
        );

        verify(operationLogMapper).insert(any(OperationLogEntity.class));
    }

    @Test
    void listLogsShouldReturnPagedRecordsForAdmin() {
        AuthenticatedUser currentUser = new AuthenticatedUser(
            99L,
            "admin@example.com",
            "Admin User",
            List.of("ADMIN")
        );

        doAnswer(invocation -> {
            Page<OperationLogEntity> page = invocation.getArgument(0);
            page.setRecords(List.of(buildLog(1L, 99L, "JOB_CREATED", "JOB")));
            page.setTotal(1);
            return page;
        }).when(operationLogMapper).selectPage(any(Page.class), any());

        PageResponse<OperationLogResponse> response = operationLogService.listLogs(
            currentUser,
            new OperationLogListRequest(1, 10, null, null, null)
        );

        assertEquals(1L, response.total());
        assertEquals("JOB_CREATED", response.records().getFirst().action());
        assertEquals(List.of("ADMIN"), response.records().getFirst().operatorRoles());
    }

    @Test
    void listLogsShouldRejectNonAdminUser() {
        AuthenticatedUser currentUser = new AuthenticatedUser(
            10L,
            "hr@example.com",
            "HR User",
            List.of("HR")
        );

        assertThrows(
            AccessDeniedException.class,
            () -> operationLogService.listLogs(currentUser, new OperationLogListRequest(1, 10, null, null, null))
        );
        verifyNoInteractions(operationLogMapper);
    }

    private OperationLogEntity buildLog(Long id, Long operatorUserId, String action, String targetType) {
        OperationLogEntity log = new OperationLogEntity();
        log.setId(id);
        log.setOperatorUserId(operatorUserId);
        log.setOperatorEmail("admin@example.com");
        log.setOperatorName("Admin User");
        log.setOperatorRoles("ADMIN");
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(100L);
        log.setDetails("Created a backend job");
        log.setCreatedAt(LocalDateTime.of(2026, 3, 15, 16, 0));
        return log;
    }
}
