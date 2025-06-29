package com.moksh.kontext.auth.exception;

import com.moksh.kontext.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class JwtSignatureInvalidException extends BusinessException {
    
    private static final AuthError AUTH_ERROR = AuthError.JWT_SIGNATURE_INVALID;
    
    public JwtSignatureInvalidException() {
        super(AUTH_ERROR, HttpStatus.UNAUTHORIZED);
    }
    
    public JwtSignatureInvalidException(String message) {
        super(message, AUTH_ERROR, HttpStatus.UNAUTHORIZED);
    }
}