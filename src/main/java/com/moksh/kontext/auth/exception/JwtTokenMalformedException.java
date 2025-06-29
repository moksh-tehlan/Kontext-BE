package com.moksh.kontext.auth.exception;

import com.moksh.kontext.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class JwtTokenMalformedException extends BusinessException {
    
    private static final AuthError AUTH_ERROR = AuthError.JWT_TOKEN_MALFORMED;
    
    public JwtTokenMalformedException() {
        super(AUTH_ERROR, HttpStatus.UNAUTHORIZED);
    }
    
    public JwtTokenMalformedException(String message) {
        super(message, AUTH_ERROR, HttpStatus.UNAUTHORIZED);
    }
}