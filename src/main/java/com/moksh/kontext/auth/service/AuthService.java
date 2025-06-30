package com.moksh.kontext.auth.service;

import com.moksh.kontext.auth.dto.*;
import com.moksh.kontext.auth.exception.*;
import com.moksh.kontext.auth.service.TokenRedisService;
import com.moksh.kontext.auth.util.JwtUtil;
import com.moksh.kontext.common.exception.BusinessException;
import com.moksh.kontext.common.exception.ResourceNotFoundException;
import com.moksh.kontext.google.dto.GoogleUserInfo;
import com.moksh.kontext.google.service.GoogleService;
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
import java.util.UUID;

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
    private final GoogleService googleService;

    public void sendOtp(SendOtpRequest request) {
        log.debug("Sending OTP to email: {}", request.getEmail());
        
        // Check if user exists
        Optional<User> existingUser = userRepository.findByEmail(request.getEmail());
        if (existingUser.isPresent()) {
            log.debug("Sending OTP to existing user: {}", request.getEmail());
        } else {
            log.debug("Sending OTP to new user (will be created upon successful login): {}", request.getEmail());
        }
        
        // Send OTP regardless of user existence (for both login and registration)
        otpService.generateAndSendOtp(request.getEmail());
        
        log.info("OTP sent successfully to email: {}", request.getEmail());
    }

    public AuthResponse loginWithOtp(LoginRequest request) {
        log.debug("Attempting OTP login for email: {}", request.getEmail());
        
        // Verify OTP
        otpService.verifyOtp(request.getEmail(), request.getOtp());
        
        // Find or create user by email
        Optional<User> existingUser = userRepository.findByEmail(request.getEmail());
        User user;
        
        if (existingUser.isPresent()) {
            user = existingUser.get();
            log.debug("Existing user found: {}", user.getEmail());
            
            // Check if user is active
            if (!user.getIsActive()) {
                throw new UserAccountDeactivatedException();
            }
        } else {
            // Create new user if doesn't exist
            user = createNewUser(request.getEmail());
            log.info("New user created and logged in: {}", user.getEmail());
        }
        
        // Verify user's email if not already verified
        if (!user.getIsEmailVerified()) {
            user.setIsEmailVerified(true);
            userRepository.save(user);
            log.info("Email verified for user: {}", user.getEmail());
        }
        
        // Revoke all existing tokens for this user (security: prevent multiple active sessions)
        tokenRedisService.revokeAllUserTokens(user.getId());
        
        // Generate new tokens
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());
        
        // Store new tokens in Redis with TTL
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
        
        // Verify Google ID token and extract user information
        GoogleUserInfo googleUserInfo = googleService.verifyGoogleToken(request.getIdToken());
        
        // Find existing user by email or Google ID
        Optional<User> existingUser = userRepository.findByEmail(googleUserInfo.getEmail());
        if (existingUser.isEmpty() && googleUserInfo.getGoogleId() != null) {
            // Also check by Google ID if email lookup fails
            existingUser = userRepository.findByGoogleId(googleUserInfo.getGoogleId());
        }
        
        User user;
        if (existingUser.isPresent()) {
            user = existingUser.get();
            log.debug("Existing user found: {}", user.getEmail());
            
            // Check if user is active
            if (!user.getIsActive()) {
                throw new UserAccountDeactivatedException();
            }
            
            // Update Google ID if not set
            if (user.getGoogleId() == null && googleUserInfo.getGoogleId() != null) {
                user.setGoogleId(googleUserInfo.getGoogleId());
            }
            
            // Update profile picture if available and not set
            if (user.getProfilePictureUrl() == null && googleUserInfo.getPictureUrl() != null) {
                user.setProfilePictureUrl(googleUserInfo.getPictureUrl());
            }
            
            // Update auth provider if it was EMAIL_OTP
            if (user.getAuthProvider() == User.AuthProvider.EMAIL_OTP) {
                user.setAuthProvider(User.AuthProvider.GOOGLE);
            }
            
            userRepository.save(user);
        } else {
            // Create new user from Google info
            user = createNewUserFromGoogle(googleUserInfo);
            log.info("New user created from Google login: {}", user.getEmail());
        }
        
        // Verify user's email if not already verified (Google provides verified emails)
        if (!user.getIsEmailVerified() && googleUserInfo.isEmailVerified()) {
            user.setIsEmailVerified(true);
            userRepository.save(user);
            log.info("Email verified for Google user: {}", user.getEmail());
        }
        
        // Revoke all existing tokens for this user (security: prevent multiple active sessions)
        tokenRedisService.revokeAllUserTokens(user.getId());
        
        // Generate new tokens
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());
        
        // Store new tokens in Redis with TTL
        tokenRedisService.storeAccessToken(user.getId(), accessToken, Duration.ofMillis(jwtUtil.getAccessTokenExpirationMs()));
        tokenRedisService.storeRefreshToken(user.getId(), refreshToken, Duration.ofMillis(jwtUtil.getRefreshTokenExpirationMs()));
        
        UserDto userDto = userMapper.toDto(user);
        
        log.info("User logged in successfully via Google: {}", user.getEmail());
        
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getAccessTokenExpirationMs() / 1000) // Convert to seconds
                .user(userDto)
                .build();
    }

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        log.debug("Refreshing access token");
        
        String refreshToken = request.getRefreshToken();
        
        // Validate refresh token
        if (!jwtUtil.isTokenValid(refreshToken) || !jwtUtil.isRefreshToken(refreshToken) || !tokenRedisService.isRefreshTokenValid(refreshToken)) {
            throw new InvalidRefreshTokenException();
        }

        // Get user from refresh token
        UUID userId = jwtUtil.getUserIdFromToken(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        // Check if user is still active
        if (!user.getIsActive()) {
            throw new UserAccountDeactivatedException();
        }
        
        // Revoke all existing access tokens for this user (security: invalidate old access tokens)
        tokenRedisService.revokeAllUserAccessTokens(user.getId());
        
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


    public void logout(UUID userId) {
        log.debug("Logging out user ID: {}", userId);
        
        // Revoke all tokens for the user
        tokenRedisService.revokeAllUserTokens(userId);
        
        log.info("User logged out successfully: {}", userId);
    }

    private User createNewUser(String email) {
        try {
            User newUser = new User();
            newUser.setEmail(email);
            
            // Extract name parts from email
            String extractedName = extractNameFromEmail(email);
            String[] nameParts = extractedName.split("\\s+");
            
            String firstName = nameParts.length > 0 && !nameParts[0].trim().isEmpty() ? nameParts[0].trim() : "User";
            String lastName = nameParts.length > 1 && !nameParts[1].trim().isEmpty() ? nameParts[1].trim() : "Account";
            
            newUser.setFirstName(firstName);
            newUser.setLastName(lastName);
            newUser.setNickname(firstName);
            
            // Set required fields
            newUser.setAuthProvider(User.AuthProvider.EMAIL_OTP);
            newUser.setRole(User.UserRole.USER);
            newUser.setIsActive(true);
            newUser.setIsEmailVerified(true); // Auto-verify since they confirmed OTP
            
            log.debug("Creating user with firstName: '{}', lastName: '{}', email: '{}'", firstName, lastName, email);
            
            User savedUser = userRepository.save(newUser);
            log.info("New user created with email: {}", email);
            return savedUser;
            
        } catch (Exception e) {
            log.error("Error creating new user with email: {}", email, e);
            throw new UserCreationFailedException("Failed to create user account", e);
        }
    }

    private User createNewUserFromGoogle(GoogleUserInfo googleUserInfo) {
        try {
            User newUser = new User();
            newUser.setEmail(googleUserInfo.getEmail());
            
            // Use Google provided names or extract from email as fallback
            String firstName = googleUserInfo.getFirstName();
            String lastName = googleUserInfo.getLastName();
            
            if (firstName == null || firstName.trim().isEmpty()) {
                String extractedName = extractNameFromEmail(googleUserInfo.getEmail());
                String[] nameParts = extractedName.split("\\s+");
                firstName = nameParts.length > 0 && !nameParts[0].trim().isEmpty() ? nameParts[0].trim() : "User";
                lastName = nameParts.length > 1 && !nameParts[1].trim().isEmpty() ? nameParts[1].trim() : "Account";
            } else if (lastName == null || lastName.trim().isEmpty()) {
                lastName = "Account";
            }
            
            newUser.setFirstName(firstName);
            newUser.setLastName(lastName);
            newUser.setNickname(firstName);
            
            // Set Google-specific fields
            newUser.setGoogleId(googleUserInfo.getGoogleId());
            newUser.setProfilePictureUrl(googleUserInfo.getPictureUrl());
            
            // Set required fields
            newUser.setAuthProvider(User.AuthProvider.GOOGLE);
            newUser.setRole(User.UserRole.USER);
            newUser.setIsActive(true);
            newUser.setIsEmailVerified(googleUserInfo.isEmailVerified());
            
            log.debug("Creating Google user with firstName: '{}', lastName: '{}', email: '{}'", firstName, lastName, googleUserInfo.getEmail());
            
            User savedUser = userRepository.save(newUser);
            log.info("New Google user created with email: {}", googleUserInfo.getEmail());
            return savedUser;
            
        } catch (Exception e) {
            log.error("Error creating new Google user with email: {}", googleUserInfo.getEmail(), e);
            throw new UserCreationFailedException("Failed to create Google user account", e);
        }
    }

    private String extractNameFromEmail(String email) {
        // Extract name from email (simple implementation)
        String[] parts = email.split("@");
        if (parts.length > 0) {
            String localPart = parts[0];
            // Replace dots, underscores, and numbers with spaces
            String cleanName = localPart.replaceAll("[._\\d]", " ").trim();
            if (cleanName.isEmpty()) {
                return "User";
            }
            return cleanName;
        }
        return "User";
    }
}