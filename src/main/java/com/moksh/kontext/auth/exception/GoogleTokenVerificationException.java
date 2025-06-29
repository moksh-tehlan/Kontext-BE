package com.moksh.kontext.auth.exception;

import com.moksh.kontext.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class GoogleTokenVerificationException extends BusinessException {
    
    private static final String DEFAULT_MESSAGE = "Failed to verify Google ID token. Please try again";
    private static final AuthError AUTH_ERROR = AuthError.GOOGLE_TOKEN_INVALID;
    
    public GoogleTokenVerificationException() {
        super(AUTH_ERROR, HttpStatus.UNAUTHORIZED);
    }
    
    public GoogleTokenVerificationException(String message) {
        super(message, AUTH_ERROR, HttpStatus.UNAUTHORIZED);
    }
    
    public GoogleTokenVerificationException(String message, Throwable cause) {
        super(String.format("%s: %s", DEFAULT_MESSAGE, message), AUTH_ERROR, HttpStatus.UNAUTHORIZED);
    }
}