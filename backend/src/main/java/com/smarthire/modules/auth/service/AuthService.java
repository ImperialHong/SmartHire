package com.smarthire.modules.auth.service;

import com.smarthire.modules.auth.dto.request.LoginRequest;
import com.smarthire.modules.auth.dto.request.RegisterRequest;
import com.smarthire.modules.auth.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);
}
