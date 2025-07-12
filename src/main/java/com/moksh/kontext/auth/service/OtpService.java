package com.moksh.kontext.auth.service;

import com.moksh.kontext.auth.exception.OtpDoesntMatchException;
import com.moksh.kontext.auth.exception.OtpExpiredException;
import com.moksh.kontext.auth.exception.OtpNotFoundException;
import com.moksh.kontext.auth.exception.OtpRateLimitExceededException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRY_MINUTES = 10;
    private static final SecureRandom random = new SecureRandom();
    
    private final EmailService emailService;
    private final OtpRateLimitService rateLimitService;
    
    // In production, use Redis or database for OTP storage
    private final ConcurrentHashMap<String, OtpData> otpStore = new ConcurrentHashMap<>();

    public void generateAndSendOtp(String email) {
        // Check rate limiting before generating OTP
        if (rateLimitService.isRateLimited(email)) {
            throw OtpRateLimitExceededException.forEmail(email);
        }
        
        // Record the attempt
        rateLimitService.recordAttempt(email);
        
        String otp = generateOtp();
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);
        
        otpStore.put(email, new OtpData(otp, expiryTime));
        
        try {
            emailService.sendOtpEmail(email, otp);
            log.info("OTP generated and sent successfully to: {} (expires at: {})", email, expiryTime);
        } catch (Exception e) {
            log.error("Failed to send OTP email to: {}", email, e);
            // Remove OTP from store if email sending fails
            otpStore.remove(email);
            throw e;
        }
    }

    public void verifyOtp(String email, String providedOtp) {
        OtpData otpData = otpStore.get(email);
        
        if (otpData == null) {
            log.debug("No OTP found for email: {}", email);
            throw OtpNotFoundException.forEmail(email);
        }
        
        if (LocalDateTime.now().isAfter(otpData.expiryTime)) {
            log.debug("OTP expired for email: {}", email);
            otpStore.remove(email);
            throw new OtpExpiredException();
        }
        
        if (!otpData.otp.equals(providedOtp)) {
            log.debug("Invalid OTP provided for email: {}", email);
            throw new OtpDoesntMatchException();
        }
        
        // Remove OTP after successful verification
        otpStore.remove(email);
        log.info("OTP verified successfully for email: {}", email);
    }

    private String generateOtp() {
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < OTP_LENGTH; i++) {
            otp.append(random.nextInt(10));
        }
        return otp.toString();
    }

    public void clearExpiredOtps() {
        LocalDateTime now = LocalDateTime.now();
        otpStore.entrySet().removeIf(entry -> now.isAfter(entry.getValue().expiryTime));
    }

    private record OtpData(String otp, LocalDateTime expiryTime) {}
}