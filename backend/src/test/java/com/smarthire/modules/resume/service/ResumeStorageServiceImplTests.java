package com.smarthire.modules.resume.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.smarthire.common.exception.BusinessException;
import com.smarthire.modules.auth.security.AuthenticatedUser;
import com.smarthire.modules.resume.dto.response.ResumeUploadResponse;
import com.smarthire.modules.resume.service.impl.ResumeStorageServiceImpl;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;

class ResumeStorageServiceImplTests {

    @TempDir
    Path tempDir;

    @Test
    void uploadResumeShouldStorePdfAndReturnLogicalPath() throws Exception {
        ResumeStorageServiceImpl service = new ResumeStorageServiceImpl(tempDir.toString(), 5_242_880L);
        AuthenticatedUser currentUser = new AuthenticatedUser(
            10L,
            "candidate@example.com",
            "Candidate User",
            List.of("CANDIDATE")
        );
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "resume.pdf",
            "application/pdf",
            "pdf-content".getBytes()
        );

        ResumeUploadResponse response = service.uploadResume(currentUser, file);

        assertEquals("resume.pdf", response.originalFilename());
        assertEquals("application/pdf", response.contentType());
        assertTrue(response.filePath().startsWith("resumes/10/"));
        assertTrue(Files.exists(tempDir.resolve("10").resolve(Path.of(response.filePath()).getFileName())));
    }

    @Test
    void uploadResumeShouldRejectNonPdfFile() {
        ResumeStorageServiceImpl service = new ResumeStorageServiceImpl(tempDir.toString(), 5_242_880L);
        AuthenticatedUser currentUser = new AuthenticatedUser(
            10L,
            "candidate@example.com",
            "Candidate User",
            List.of("CANDIDATE")
        );
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "resume.txt",
            "text/plain",
            "plain-text".getBytes()
        );

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> service.uploadResume(currentUser, file)
        );

        assertEquals("INVALID_RESUME_FILE_TYPE", exception.getCode());
    }

    @Test
    void uploadResumeShouldRejectOversizedFile() {
        ResumeStorageServiceImpl service = new ResumeStorageServiceImpl(tempDir.toString(), 5L);
        AuthenticatedUser currentUser = new AuthenticatedUser(
            10L,
            "candidate@example.com",
            "Candidate User",
            List.of("CANDIDATE")
        );
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "resume.pdf",
            "application/pdf",
            "too-large".getBytes()
        );

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> service.uploadResume(currentUser, file)
        );

        assertEquals("RESUME_FILE_TOO_LARGE", exception.getCode());
    }

    @Test
    void uploadResumeShouldRejectNonCandidateUser() {
        ResumeStorageServiceImpl service = new ResumeStorageServiceImpl(tempDir.toString(), 5_242_880L);
        AuthenticatedUser currentUser = new AuthenticatedUser(
            20L,
            "hr@example.com",
            "HR User",
            List.of("HR")
        );
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "resume.pdf",
            "application/pdf",
            "pdf-content".getBytes()
        );

        assertThrows(AccessDeniedException.class, () -> service.uploadResume(currentUser, file));
    }
}
