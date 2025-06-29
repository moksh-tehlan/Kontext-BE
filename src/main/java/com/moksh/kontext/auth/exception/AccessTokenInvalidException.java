package com.moksh.kontext.auth.exception;

import com.moksh.kontext.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class AccessTokenInvalidException extends BusinessException {
    
    private static final AuthError AUTH_ERROR = AuthError.ACCESS_TOKEN_INVALID;
    
    public AccessTokenInvalidException() {
        super(AUTH_ERROR, HttpStatus.UNAUTHORIZED);
    }
    
    public AccessTokenInvalidException(String message) {
        super(message, AUTH_ERROR, HttpStatus.UNAUTHORIZED);
    }
}