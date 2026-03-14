package com.smarthire.modules.application.controller;

import com.smarthire.common.api.ApiResponse;
import com.smarthire.common.api.PageResponse;
import com.smarthire.modules.application.dto.request.ApplicationCreateRequest;
import com.smarthire.modules.application.dto.request.ApplicationListRequest;
import com.smarthire.modules.application.dto.request.ApplicationStatusUpdateRequest;
import com.smarthire.modules.application.dto.response.ApplicationResponse;
import com.smarthire.modules.application.service.ApplicationService;
import com.smarthire.modules.auth.security.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping("/applications")
    public ApiResponse<ApplicationResponse> apply(
        @AuthenticationPrincipal AuthenticatedUser currentUser,
        @Valid @RequestBody ApplicationCreateRequest request
    ) {
        return ApiResponse.success("Application submitted successfully", applicationService.apply(currentUser, request));
    }

    @GetMapping("/applications/me")
    public ApiResponse<PageResponse<ApplicationResponse>> listMyApplications(
        @AuthenticationPrincipal AuthenticatedUser currentUser,
        @RequestParam(defaultValue = "1") @Min(value = 1, message = "page must be greater than 0") long page,
        @RequestParam(defaultValue = "10") @Min(value = 1, message = "size must be greater than 0")
        @Max(value = 100, message = "size must be less than or equal to 100") long size,
        @RequestParam(required = false) String status
    ) {
        return ApiResponse.success(applicationService.listMyApplications(
            currentUser,
            new ApplicationListRequest(page, size, status)
        ));
    }

    @GetMapping("/jobs/{jobId}/applications")
    public ApiResponse<PageResponse<ApplicationResponse>> listJobApplications(
        @AuthenticationPrincipal AuthenticatedUser currentUser,
        @PathVariable Long jobId,
        @RequestParam(defaultValue = "1") @Min(value = 1, message = "page must be greater than 0") long page,
        @RequestParam(defaultValue = "10") @Min(value = 1, message = "size must be greater than 0")
        @Max(value = 100, message = "size must be less than or equal to 100") long size,
        @RequestParam(required = false) String status
    ) {
        return ApiResponse.success(applicationService.listJobApplications(
            currentUser,
            jobId,
            new ApplicationListRequest(page, size, status)
        ));
    }

    @PatchMapping("/applications/{id}/status")
    public ApiResponse<ApplicationResponse> updateStatus(
        @AuthenticationPrincipal AuthenticatedUser currentUser,
        @PathVariable Long id,
        @Valid @RequestBody ApplicationStatusUpdateRequest request
    ) {
        return ApiResponse.success(
            "Application status updated successfully",
            applicationService.updateStatus(currentUser, id, request)
        );
    }
}
