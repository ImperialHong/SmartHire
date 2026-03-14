package com.smarthire.modules.application.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ApplicationCreateRequest(
    @NotNull(message = "jobId is required")
    Long jobId,

    @Size(max = 255, message = "resumeFilePath length must be less than or equal to 255")
    String resumeFilePath,

    @Size(max = 2000, message = "coverLetter length must be less than or equal to 2000")
    String coverLetter
) {
}
