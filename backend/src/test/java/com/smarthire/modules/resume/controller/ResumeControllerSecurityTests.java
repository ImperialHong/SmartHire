package com.smarthire.modules.resume.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.smarthire.modules.auth.security.AuthenticatedUser;
import com.smarthire.modules.auth.security.JwtTokenService;
import com.smarthire.modules.resume.dto.response.ResumeUploadResponse;
import com.smarthire.modules.resume.service.ResumeStorageService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ResumeControllerSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    @MockBean(name = "resumeStorageServiceImpl")
    private ResumeStorageService resumeStorageService;

    @Test
    void uploadResumeShouldRequireAuthentication() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "resume.pdf",
            "application/pdf",
            "pdf-content".getBytes()
        );

        mockMvc.perform(multipart("/api/resumes/upload").file(file))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void uploadResumeShouldRejectHrRole() throws Exception {
        String token = jwtTokenService.generateToken(
            new AuthenticatedUser(2L, "hr@example.com", "HR User", List.of("HR"))
        );
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "resume.pdf",
            "application/pdf",
            "pdf-content".getBytes()
        );

        mockMvc.perform(multipart("/api/resumes/upload")
                .file(file)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void uploadResumeShouldAllowCandidateRole() throws Exception {
        String token = jwtTokenService.generateToken(
            new AuthenticatedUser(1L, "candidate@example.com", "Candidate User", List.of("CANDIDATE"))
        );
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "resume.pdf",
            "application/pdf",
            "pdf-content".getBytes()
        );

        when(resumeStorageService.uploadResume(any(), any())).thenReturn(new ResumeUploadResponse(
            "resumes/1/resume-abc.pdf",
            "resume.pdf",
            11L,
            "application/pdf",
            LocalDateTime.of(2026, 3, 15, 12, 0)
        ));

        mockMvc.perform(multipart("/api/resumes/upload")
                .file(file)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.filePath").value("resumes/1/resume-abc.pdf"));
    }
}
