package com.smarthire.modules.operationlog.controller;

import com.smarthire.common.api.ApiResponse;
import com.smarthire.common.api.PageResponse;
import com.smarthire.modules.auth.security.AuthenticatedUser;
import com.smarthire.modules.operationlog.dto.request.OperationLogListRequest;
import com.smarthire.modules.operationlog.dto.response.OperationLogResponse;
import com.smarthire.modules.operationlog.service.OperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@Tag(name = "Operation Logs", description = "Lightweight admin audit logs")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/admin/operation-logs")
public class OperationLogController {

    private final OperationLogService operationLogService;

    public OperationLogController(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @Operation(summary = "List operation logs")
    @GetMapping
    public ApiResponse<PageResponse<OperationLogResponse>> listLogs(
        @AuthenticationPrincipal AuthenticatedUser currentUser,
        @RequestParam(defaultValue = "1") @Min(value = 1, message = "page must be greater than 0") long page,
        @RequestParam(defaultValue = "10") @Min(value = 1, message = "size must be greater than 0")
        @Max(value = 100, message = "size must be less than or equal to 100") long size,
        @RequestParam(required = false) String action,
        @RequestParam(required = false) String targetType,
        @RequestParam(required = false) Long operatorUserId
    ) {
        return ApiResponse.success(operationLogService.listLogs(
            currentUser,
            new OperationLogListRequest(page, size, action, targetType, operatorUserId)
        ));
    }
}
