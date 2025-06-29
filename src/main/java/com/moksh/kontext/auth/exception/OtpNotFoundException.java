package com.moksh.kontext.auth.exception;

import com.moksh.kontext.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class OtpNotFoundException extends BusinessException {
    
    private static final String DEFAULT_MESSAGE = "No OTP found for this email. Please request a new OTP";
    private static final AuthError AUTH_ERROR = AuthError.OTP_NOT_FOUND;
    
    public OtpNotFoundException() {
        super(AUTH_ERROR, HttpStatus.BAD_REQUEST);
    }
    
    public OtpNotFoundException(String message) {
        super(message, AUTH_ERROR, HttpStatus.BAD_REQUEST);
    }
    
    public static OtpNotFoundException forEmail(String email) {
        return new OtpNotFoundException(String.format("No OTP found for email: %s. Please request a new OTP", email));
    }
}