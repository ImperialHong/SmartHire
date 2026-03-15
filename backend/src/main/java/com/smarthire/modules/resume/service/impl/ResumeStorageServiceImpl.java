package com.smarthire.modules.resume.service.impl;

import com.smarthire.common.exception.BusinessException;
import com.smarthire.modules.auth.security.AuthenticatedUser;
import com.smarthire.modules.resume.dto.response.ResumeUploadResponse;
import com.smarthire.modules.resume.service.ResumeStorageService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ResumeStorageServiceImpl implements ResumeStorageService {

    private static final String LOGICAL_PREFIX = "resumes";
    private static final List<String> ALLOWED_CONTENT_TYPES = List.of("application/pdf");

    private final Path storageRoot;
    private final long resumeMaxSizeBytes;

    public ResumeStorageServiceImpl(
        @Value("${app.storage.resume-dir:uploads/resumes}") String resumeDir,
        @Value("${app.storage.resume-max-size-bytes:5242880}") long resumeMaxSizeBytes
    ) {
        this.storageRoot = Path.of(resumeDir).toAbsolutePath().normalize();
        this.resumeMaxSizeBytes = resumeMaxSizeBytes;
    }

    @Override
    public ResumeUploadResponse uploadResume(AuthenticatedUser currentUser, MultipartFile file) {
        validateCandidateRole(currentUser);
        validateFile(file);

        String storedFilename = buildStoredFilename(file.getOriginalFilename());
        Path candidateDirectory = storageRoot.resolve(String.valueOf(currentUser.userId())).normalize();
        Path targetPath = candidateDirectory.resolve(storedFilename).normalize();
        if (!targetPath.startsWith(candidateDirectory)) {
            throw new BusinessException("INVALID_RESUME_PATH", "Invalid resume storage path");
        }

        try {
            Files.createDirectories(candidateDirectory);
            file.transferTo(targetPath);
        } catch (IOException ex) {
            throw new BusinessException("RESUME_UPLOAD_FAILED", "Failed to store resume file");
        }

        LocalDateTime uploadedAt = LocalDateTime.now();
        String logicalPath = LOGICAL_PREFIX + "/" + currentUser.userId() + "/" + storedFilename;
        return new ResumeUploadResponse(
            logicalPath,
            file.getOriginalFilename(),
            file.getSize(),
            file.getContentType(),
            uploadedAt
        );
    }

    private void validateCandidateRole(AuthenticatedUser currentUser) {
        if (currentUser == null || currentUser.roles() == null || !currentUser.roles().contains("CANDIDATE")) {
            throw new AccessDeniedException("Only candidates can upload resumes");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("RESUME_FILE_REQUIRED", "Resume PDF file is required");
        }

        if (file.getSize() > resumeMaxSizeBytes) {
            throw new BusinessException(
                "RESUME_FILE_TOO_LARGE",
                "Resume file size must be less than or equal to " + resumeMaxSizeBytes + " bytes"
            );
        }

        String originalFilename = file.getOriginalFilename();
        if (!StringUtils.hasText(originalFilename)
            || !originalFilename.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw new BusinessException("INVALID_RESUME_FILE_TYPE", "Only PDF resume files are supported");
        }

        String contentType = file.getContentType();
        if (StringUtils.hasText(contentType) && !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BusinessException("INVALID_RESUME_FILE_TYPE", "Only PDF resume files are supported");
        }
    }

    private String buildStoredFilename(String originalFilename) {
        String extension = ".pdf";
        if (StringUtils.hasText(originalFilename) && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase(Locale.ROOT);
        }

        return "resume-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().replace("-", "")
            + extension;
    }
}
