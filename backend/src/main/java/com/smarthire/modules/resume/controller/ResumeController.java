package com.smarthire.modules.resume.controller;

import com.smarthire.common.api.ApiResponse;
import com.smarthire.modules.auth.security.AuthenticatedUser;
import com.smarthire.modules.resume.dto.response.ResumeUploadResponse;
import com.smarthire.modules.resume.service.ResumeStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Resumes", description = "Candidate resume upload")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/resumes")
public class ResumeController {

    private final ResumeStorageService resumeStorageService;

    public ResumeController(ResumeStorageService resumeStorageService) {
        this.resumeStorageService = resumeStorageService;
    }

    @Operation(
        summary = "Upload a PDF resume file",
        requestBody = @RequestBody(content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE))
    )
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ResumeUploadResponse> uploadResume(
        @AuthenticationPrincipal AuthenticatedUser currentUser,
        @RequestPart("file") MultipartFile file
    ) {
        return ApiResponse.success(
            "Resume uploaded successfully",
            resumeStorageService.uploadResume(currentUser, file)
        );
    }
}
