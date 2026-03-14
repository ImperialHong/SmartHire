package com.smarthire.modules.job.controller;

import com.smarthire.common.api.ApiResponse;
import com.smarthire.common.api.PageResponse;
import com.smarthire.modules.auth.security.AuthenticatedUser;
import com.smarthire.modules.job.dto.request.JobCreateRequest;
import com.smarthire.modules.job.dto.request.JobSearchRequest;
import com.smarthire.modules.job.dto.request.JobUpdateRequest;
import com.smarthire.modules.job.dto.response.JobResponse;
import com.smarthire.modules.job.service.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@Tag(name = "Jobs", description = "Public job browsing and recruiter job management")
@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @Operation(
        summary = "Create a job posting",
        security = {@SecurityRequirement(name = "bearerAuth")}
    )
    @PostMapping
    public ApiResponse<JobResponse> createJob(
        @AuthenticationPrincipal AuthenticatedUser currentUser,
        @Valid @RequestBody JobCreateRequest request
    ) {
        return ApiResponse.success("Job created successfully", jobService.createJob(currentUser, request));
    }

    @Operation(
        summary = "Update a job posting",
        security = {@SecurityRequirement(name = "bearerAuth")}
    )
    @PutMapping("/{id}")
    public ApiResponse<JobResponse> updateJob(
        @AuthenticationPrincipal AuthenticatedUser currentUser,
        @PathVariable Long id,
        @Valid @RequestBody JobUpdateRequest request
    ) {
        return ApiResponse.success("Job updated successfully", jobService.updateJob(currentUser, id, request));
    }

    @Operation(
        summary = "Delete a job posting",
        security = {@SecurityRequirement(name = "bearerAuth")}
    )
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteJob(
        @AuthenticationPrincipal AuthenticatedUser currentUser,
        @PathVariable Long id
    ) {
        jobService.deleteJob(currentUser, id);
        return ApiResponse.success("Job deleted successfully", null);
    }

    @Operation(summary = "Get job details")
    @GetMapping("/{id}")
    public ApiResponse<JobResponse> getJob(@PathVariable Long id) {
        return ApiResponse.success(jobService.getJob(id));
    }

    @Operation(summary = "List jobs with pagination and filters")
    @GetMapping
    public ApiResponse<PageResponse<JobResponse>> listJobs(
        @RequestParam(defaultValue = "1") @Min(value = 1, message = "page must be greater than 0") long page,
        @RequestParam(defaultValue = "10") @Min(value = 1, message = "size must be greater than 0")
        @Max(value = 100, message = "size must be less than or equal to 100") long size,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String city,
        @RequestParam(required = false) String category,
        @RequestParam(required = false) String status
    ) {
        JobSearchRequest request = new JobSearchRequest(page, size, keyword, city, category, status);
        return ApiResponse.success(jobService.listJobs(request));
    }
}
