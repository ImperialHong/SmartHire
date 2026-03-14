package com.smarthire.modules.auth.controller;

import com.smarthire.common.api.ApiResponse;
import com.smarthire.modules.auth.dto.request.LoginRequest;
import com.smarthire.modules.auth.dto.request.RegisterRequest;
import com.smarthire.modules.auth.dto.response.AuthResponse;
import com.smarthire.modules.auth.dto.response.UserProfileResponse;
import com.smarthire.modules.auth.security.AuthenticatedUser;
import com.smarthire.modules.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success("Registration succeeded", authService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success("Login succeeded", authService.login(request));
    }

    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> me(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(UserProfileResponse.fromAuthenticatedUser(currentUser));
    }
}
