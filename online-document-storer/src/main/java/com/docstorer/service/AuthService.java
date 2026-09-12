package com.docstorer.service;

import com.docstorer.dto.request.LoginRequest;
import com.docstorer.dto.request.RegisterRequest;
import com.docstorer.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request, String ipAddress, String userAgent);
    AuthResponse refreshToken(String refreshToken);
    void logout(String token);
}
