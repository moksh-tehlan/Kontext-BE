package com.moksh.kontext.auth.exception;

import com.moksh.kontext.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class OtpExpiredException extends BusinessException {
    
    private static final String DEFAULT_MESSAGE = "The OTP has expired. Please request a new one";
    private static final AuthError AUTH_ERROR = AuthError.OTP_EXPIRED;
    
    public OtpExpiredException() {
        super(AUTH_ERROR, HttpStatus.BAD_REQUEST);
    }
    
    public OtpExpiredException(String message) {
        super(message, AUTH_ERROR, HttpStatus.BAD_REQUEST);
    }
}