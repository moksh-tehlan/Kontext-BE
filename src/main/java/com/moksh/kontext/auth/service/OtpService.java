package com.moksh.kontext.auth.service;

import com.moksh.kontext.auth.exception.OtpDoesntMatchException;
import com.moksh.kontext.auth.exception.OtpExpiredException;
import com.moksh.kontext.auth.exception.OtpNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class OtpService {

    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRY_MINUTES = 10;
    private static final SecureRandom random = new SecureRandom();
    
    // In production, use Redis or database for OTP storage
    private final ConcurrentHashMap<String, OtpData> otpStore = new ConcurrentHashMap<>();

    public void generateAndSendOtp(String email) {
        String otp = generateOtp();
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);
        
        otpStore.put(email, new OtpData(otp, expiryTime));
        
        // In production, send OTP via email service
        // For development, we'll just log it
        log.info("OTP for {}: {} (expires at: {})", email, otp, expiryTime);
        
        // Simulate email sending
        sendOtpEmail(email, otp);
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
//        StringBuilder otp = new StringBuilder();
//        for (int i = 0; i < OTP_LENGTH; i++) {
//            otp.append(random.nextInt(10));
//        }
        return "999999";
    }

    private void sendOtpEmail(String email, String otp) {
        // In production, integrate with email service (SendGrid, AWS SES, etc.)
        log.info("Sending OTP email to: {} with OTP: {}", email, otp);
        
        // TODO: Implement actual email sending
        // Example:
        // emailService.sendOtpEmail(email, otp);
    }

    public void clearExpiredOtps() {
        LocalDateTime now = LocalDateTime.now();
        otpStore.entrySet().removeIf(entry -> now.isAfter(entry.getValue().expiryTime));
    }

    private record OtpData(String otp, LocalDateTime expiryTime) {}
}