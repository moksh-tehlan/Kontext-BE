package com.moksh.kontext.auth.exception;

import com.moksh.kontext.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class InvalidRefreshTokenException extends BusinessException {
    
    private static final String DEFAULT_MESSAGE = "Invalid or expired refresh token. Please login again";
    private static final AuthError AUTH_ERROR = AuthError.INVALID_REFRESH_TOKEN;
    
    public InvalidRefreshTokenException() {
        super(AUTH_ERROR, HttpStatus.UNAUTHORIZED);
    }
    
    public InvalidRefreshTokenException(String message) {
        super(message, AUTH_ERROR, HttpStatus.UNAUTHORIZED);
    }
}