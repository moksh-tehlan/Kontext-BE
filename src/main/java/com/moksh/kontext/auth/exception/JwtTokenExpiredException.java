package com.moksh.kontext.auth.exception;

import com.moksh.kontext.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class JwtTokenExpiredException extends BusinessException {
    
    private static final AuthError AUTH_ERROR = AuthError.JWT_TOKEN_EXPIRED;
    
    public JwtTokenExpiredException() {
        super(AUTH_ERROR, HttpStatus.UNAUTHORIZED);
    }
    
    public JwtTokenExpiredException(String message) {
        super(message, AUTH_ERROR, HttpStatus.UNAUTHORIZED);
    }
}