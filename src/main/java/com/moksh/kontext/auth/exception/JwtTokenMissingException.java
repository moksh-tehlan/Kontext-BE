package com.moksh.kontext.auth.exception;

import com.moksh.kontext.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class JwtTokenMissingException extends BusinessException {
    
    private static final AuthError AUTH_ERROR = AuthError.JWT_TOKEN_MISSING;
    
    public JwtTokenMissingException() {
        super(AUTH_ERROR, HttpStatus.UNAUTHORIZED);
    }
    
    public JwtTokenMissingException(String message) {
        super(message, AUTH_ERROR, HttpStatus.UNAUTHORIZED);
    }
}