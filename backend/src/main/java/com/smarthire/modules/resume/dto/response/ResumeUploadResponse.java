package com.smarthire.modules.resume.dto.response;

import java.time.LocalDateTime;

public record ResumeUploadResponse(
    String filePath,
    String originalFilename,
    long fileSize,
    String contentType,
    LocalDateTime uploadedAt
) {
}
