package com.smarthire.modules.interview.controller;

import com.smarthire.common.api.ApiResponse;
import com.smarthire.common.api.PageResponse;
import com.smarthire.modules.auth.security.AuthenticatedUser;
import com.smarthire.modules.interview.dto.request.InterviewCreateRequest;
import com.smarthire.modules.interview.dto.request.InterviewListRequest;
import com.smarthire.modules.interview.dto.request.InterviewUpdateRequest;
import com.smarthire.modules.interview.dto.response.InterviewResponse;
import com.smarthire.modules.interview.service.InterviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Interviews", description = "Interview scheduling and follow-up management")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api")
public class InterviewController {

    private final InterviewService interviewService;

    public InterviewController(InterviewService interviewService) {
        this.interviewService = interviewService;
    }

    @Operation(summary = "Schedule an interview for an application")
    @PostMapping("/interviews")
    public ApiResponse<InterviewResponse> scheduleInterview(
        @AuthenticationPrincipal AuthenticatedUser currentUser,
        @Valid @RequestBody InterviewCreateRequest request
    ) {
        return ApiResponse.success(
            "Interview scheduled successfully",
            interviewService.scheduleInterview(currentUser, request)
        );
    }

    @Operation(summary = "Update interview details, status or result")
    @PatchMapping("/interviews/{id}")
    public ApiResponse<InterviewResponse> updateInterview(
        @AuthenticationPrincipal AuthenticatedUser currentUser,
        @PathVariable Long id,
        @Valid @RequestBody InterviewUpdateRequest request
    ) {
        return ApiResponse.success(
            "Interview updated successfully",
            interviewService.updateInterview(currentUser, id, request)
        );
    }

    @Operation(summary = "List the current candidate's interviews")
    @GetMapping("/interviews/me")
    public ApiResponse<PageResponse<InterviewResponse>> listMyInterviews(
        @AuthenticationPrincipal AuthenticatedUser currentUser,
        @RequestParam(defaultValue = "1") @Min(value = 1, message = "page must be greater than 0") long page,
        @RequestParam(defaultValue = "10") @Min(value = 1, message = "size must be greater than 0")
        @Max(value = 100, message = "size must be less than or equal to 100") long size,
        @RequestParam(required = false) String status
    ) {
        return ApiResponse.success(interviewService.listMyInterviews(
            currentUser,
            new InterviewListRequest(page, size, status)
        ));
    }

    @Operation(summary = "List interviews under a specific job")
    @GetMapping("/jobs/{jobId}/interviews")
    public ApiResponse<PageResponse<InterviewResponse>> listJobInterviews(
        @AuthenticationPrincipal AuthenticatedUser currentUser,
        @PathVariable Long jobId,
        @RequestParam(defaultValue = "1") @Min(value = 1, message = "page must be greater than 0") long page,
        @RequestParam(defaultValue = "10") @Min(value = 1, message = "size must be greater than 0")
        @Max(value = 100, message = "size must be less than or equal to 100") long size,
        @RequestParam(required = false) String status
    ) {
        return ApiResponse.success(interviewService.listJobInterviews(
            currentUser,
            jobId,
            new InterviewListRequest(page, size, status)
        ));
    }
}
