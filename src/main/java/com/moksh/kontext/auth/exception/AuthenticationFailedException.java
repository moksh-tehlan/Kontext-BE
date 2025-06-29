package com.moksh.kontext.auth.exception;

import com.moksh.kontext.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class AuthenticationFailedException extends BusinessException {
    
    private static final String DEFAULT_MESSAGE = "Authentication failed";
    private static final AuthError AUTH_ERROR = AuthError.AUTHENTICATION_FAILED;
    
    public AuthenticationFailedException() {
        super(AUTH_ERROR, HttpStatus.UNAUTHORIZED);
    }
    
    public AuthenticationFailedException(String message) {
        super(message, AUTH_ERROR, HttpStatus.UNAUTHORIZED);
    }
}