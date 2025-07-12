package com.moksh.kontext.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpCleanupService {

    private final OtpService otpService;

    @Scheduled(fixedRate = 300000) // Run every 5 minutes
    public void cleanupExpiredOtps() {
        log.debug("Starting OTP cleanup task");
        otpService.clearExpiredOtps();
        log.debug("OTP cleanup task completed");
    }
}