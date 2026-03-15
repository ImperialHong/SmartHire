package com.smarthire.modules.resume.service;

import com.smarthire.modules.auth.security.AuthenticatedUser;
import com.smarthire.modules.resume.dto.response.ResumeUploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface ResumeStorageService {

    ResumeUploadResponse uploadResume(AuthenticatedUser currentUser, MultipartFile file);
}
