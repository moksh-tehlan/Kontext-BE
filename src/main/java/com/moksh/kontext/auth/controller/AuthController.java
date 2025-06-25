package com.moksh.kontext.auth.controller;

import com.moksh.kontext.auth.dto.*;
import com.moksh.kontext.auth.service.AuthService;
import com.moksh.kontext.common.util.SecurityContextUtil;
import com.moksh.kontext.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/send-otp")
    public ApiResponse<Void> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        log.debug("POST /api/auth/send-otp - Sending OTP to email: {}", request.getEmail());
        
        authService.sendOtp(request);
        return ApiResponse.success(null, "OTP sent successfully");
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.debug("POST /api/auth/login - User login attempt for email: {}", request.getEmail());
        
        AuthResponse authResponse = authService.loginWithOtp(request);
        return ApiResponse.success(authResponse, "Login successful");
    }

    @PostMapping("/google")
    public ApiResponse<AuthResponse> googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
        log.debug("POST /api/auth/google - Google login attempt");
        
        AuthResponse authResponse = authService.loginWithGoogle(request);
        return ApiResponse.success(authResponse, "Google login successful");
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        log.debug("POST /api/auth/refresh - Refreshing access token");
        
        AuthResponse authResponse = authService.refreshToken(request);
        return ApiResponse.success(authResponse, "Token refreshed successfully");
    }


    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        log.debug("POST /api/auth/logout - User logout");
        
        authService.logout(SecurityContextUtil.getCurrentUserIdOrThrow());
        return ApiResponse.success(null, "Logout successful");
    }
}