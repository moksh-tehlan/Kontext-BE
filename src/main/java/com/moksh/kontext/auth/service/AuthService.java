package com.moksh.kontext.auth.service;

import com.moksh.kontext.auth.dto.*;
import com.moksh.kontext.auth.service.TokenRedisService;
import com.moksh.kontext.auth.util.JwtUtil;
import com.moksh.kontext.common.exception.BusinessException;
import com.moksh.kontext.common.exception.ResourceNotFoundException;
import com.moksh.kontext.user.dto.UserDto;
import com.moksh.kontext.user.entity.User;
import com.moksh.kontext.user.mapper.UserMapper;
import com.moksh.kontext.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final OtpService otpService;
    private final TokenRedisService tokenRedisService;

    public void sendOtp(SendOtpRequest request) {
        log.debug("Sending OTP to email: {}", request.getEmail());
        
        // For existing users, just send OTP
        // For new users, this will be used during registration
        otpService.generateAndSendOtp(request.getEmail());
        
        log.info("OTP sent successfully to email: {}", request.getEmail());
    }

    public AuthResponse loginWithOtp(LoginRequest request) {
        log.debug("Attempting OTP login for email: {}", request.getEmail());
        
        // Verify OTP
        if (!otpService.verifyOtp(request.getEmail(), request.getOtp())) {
            throw new BusinessException("Invalid or expired OTP");
        }
        
        // Find user by email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.getEmail()));
        
        // Check if user is active
        if (!user.getIsActive()) {
            throw new BusinessException("User account is deactivated");
        }
        
        // Verify user's email if not already verified
        if (!user.getIsEmailVerified()) {
            user.setIsEmailVerified(true);
            userRepository.save(user);
            log.info("Email verified for user: {}", user.getEmail());
        }
        
        // Generate tokens
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());
        
        // Store tokens in Redis with TTL
        tokenRedisService.storeAccessToken(user.getId(), accessToken, Duration.ofMillis(jwtUtil.getAccessTokenExpirationMs()));
        tokenRedisService.storeRefreshToken(user.getId(), refreshToken, Duration.ofMillis(jwtUtil.getRefreshTokenExpirationMs()));
        
        UserDto userDto = userMapper.toDto(user);
        
        log.info("User logged in successfully: {}", user.getEmail());
        
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getAccessTokenExpirationMs() / 1000) // Convert to seconds
                .user(userDto)
                .build();
    }

    public AuthResponse loginWithGoogle(GoogleLoginRequest request) {
        log.debug("Attempting Google login with ID token");
        
        // In a real implementation, you would validate the Google ID token here
        // For now, we'll assume the token is valid and extract user info
        // This is a placeholder - implement Google token validation
        throw new BusinessException("Google login not implemented yet");
    }

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        log.debug("Refreshing access token");
        
        String refreshToken = request.getRefreshToken();
        
        // Validate refresh token
        if (!jwtUtil.isTokenValid(refreshToken) || !jwtUtil.isRefreshToken(refreshToken) || !tokenRedisService.isRefreshTokenValid(refreshToken)) {
            throw new BusinessException("Invalid refresh token");
        }
        
        // Get user from refresh token
        Long userId = jwtUtil.getUserIdFromToken(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        // Check if user is still active
        if (!user.getIsActive()) {
            throw new BusinessException("User account is deactivated");
        }
        
        // Generate new access token
        String newAccessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail());
        
        // Store new access token in Redis
        tokenRedisService.storeAccessToken(user.getId(), newAccessToken, Duration.ofMillis(jwtUtil.getAccessTokenExpirationMs()));
        
        UserDto userDto = userMapper.toDto(user);
        
        log.info("Access token refreshed for user: {}", user.getEmail());
        
        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken) // Keep the same refresh token
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getAccessTokenExpirationMs() / 1000)
                .user(userDto)
                .build();
    }


    public void logout(Long userId) {
        log.debug("Logging out user ID: {}", userId);
        
        // Revoke all tokens for the user
        tokenRedisService.revokeAllUserTokens(userId);
        
        log.info("User logged out successfully: {}", userId);
    }
}