package com.smarthire.modules.admin.controller;

import com.smarthire.common.api.ApiResponse;
import com.smarthire.common.api.PageResponse;
import com.smarthire.modules.admin.dto.request.AdminJobListRequest;
import com.smarthire.modules.admin.dto.response.AdminJobResponse;
import com.smarthire.modules.admin.service.AdminJobService;
import com.smarthire.modules.auth.security.AuthenticatedUser;
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
@Tag(name = "Admin Jobs", description = "Lightweight admin job oversight")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/admin/jobs")
public class AdminJobController {

    private final AdminJobService adminJobService;

    public AdminJobController(AdminJobService adminJobService) {
        this.adminJobService = adminJobService;
    }

    @Operation(summary = "List jobs for admin oversight")
    @GetMapping
    public ApiResponse<PageResponse<AdminJobResponse>> listJobs(
        @AuthenticationPrincipal AuthenticatedUser currentUser,
        @RequestParam(defaultValue = "1") @Min(value = 1, message = "page must be greater than 0") long page,
        @RequestParam(defaultValue = "10") @Min(value = 1, message = "size must be greater than 0")
        @Max(value = 100, message = "size must be less than or equal to 100") long size,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String ownerKeyword
    ) {
        return ApiResponse.success(adminJobService.listJobs(
            currentUser,
            new AdminJobListRequest(page, size, keyword, status, ownerKeyword)
        ));
    }
}
