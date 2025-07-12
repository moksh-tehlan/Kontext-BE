package com.moksh.kontext.auth.exception;

public class OtpRateLimitExceededException extends RuntimeException {
    public OtpRateLimitExceededException(String message) {
        super(message);
    }
    
    public static OtpRateLimitExceededException forEmail(String email) {
        return new OtpRateLimitExceededException("Rate limit exceeded for email: " + email + ". Please try again later.");
    }
}