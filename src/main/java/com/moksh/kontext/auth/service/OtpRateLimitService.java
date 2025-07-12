package com.moksh.kontext.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class OtpRateLimitService {

    private static final int MAX_ATTEMPTS_PER_HOUR = 5;
    private static final int RATE_LIMIT_WINDOW_MINUTES = 60;
    
    private final ConcurrentHashMap<String, RateLimitData> rateLimitStore = new ConcurrentHashMap<>();

    public boolean isRateLimited(String email) {
        cleanupExpiredEntries();
        
        RateLimitData rateLimitData = rateLimitStore.get(email);
        
        if (rateLimitData == null) {
            return false;
        }
        
        LocalDateTime now = LocalDateTime.now();
        
        // Check if the window has expired
        if (now.isAfter(rateLimitData.windowStart.plusMinutes(RATE_LIMIT_WINDOW_MINUTES))) {
            rateLimitStore.remove(email);
            return false;
        }
        
        boolean isLimited = rateLimitData.attemptCount >= MAX_ATTEMPTS_PER_HOUR;
        
        if (isLimited) {
            log.warn("Rate limit exceeded for email: {}. Attempts: {}", email, rateLimitData.attemptCount);
        }
        
        return isLimited;
    }

    public void recordAttempt(String email) {
        LocalDateTime now = LocalDateTime.now();
        
        rateLimitStore.compute(email, (key, existing) -> {
            if (existing == null) {
                return new RateLimitData(now, 1);
            }
            
            // If the window has expired, reset the counter
            if (now.isAfter(existing.windowStart.plusMinutes(RATE_LIMIT_WINDOW_MINUTES))) {
                return new RateLimitData(now, 1);
            }
            
            // Increment the counter
            return new RateLimitData(existing.windowStart, existing.attemptCount + 1);
        });
        
        log.debug("Recorded OTP attempt for email: {}", email);
    }

    public int getRemainingAttempts(String email) {
        RateLimitData rateLimitData = rateLimitStore.get(email);
        
        if (rateLimitData == null) {
            return MAX_ATTEMPTS_PER_HOUR;
        }
        
        LocalDateTime now = LocalDateTime.now();
        
        // Check if the window has expired
        if (now.isAfter(rateLimitData.windowStart.plusMinutes(RATE_LIMIT_WINDOW_MINUTES))) {
            return MAX_ATTEMPTS_PER_HOUR;
        }
        
        return Math.max(0, MAX_ATTEMPTS_PER_HOUR - rateLimitData.attemptCount);
    }

    private void cleanupExpiredEntries() {
        LocalDateTime now = LocalDateTime.now();
        rateLimitStore.entrySet().removeIf(entry -> 
            now.isAfter(entry.getValue().windowStart.plusMinutes(RATE_LIMIT_WINDOW_MINUTES))
        );
    }

    private record RateLimitData(LocalDateTime windowStart, int attemptCount) {}
}