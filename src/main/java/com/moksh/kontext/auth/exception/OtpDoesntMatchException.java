package com.moksh.kontext.auth.exception;

import com.moksh.kontext.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class OtpDoesntMatchException extends BusinessException {
    
    private static final String DEFAULT_MESSAGE = "The OTP provided does not match";
    private static final AuthError AUTH_ERROR = AuthError.OTP_MISMATCH;
    
    public OtpDoesntMatchException() {
        super(AUTH_ERROR, HttpStatus.BAD_REQUEST);
    }
    
    public OtpDoesntMatchException(String message) {
        super(message, AUTH_ERROR, HttpStatus.BAD_REQUEST);
    }
}