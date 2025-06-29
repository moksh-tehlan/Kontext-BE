package com.moksh.kontext.auth.exception;

import com.moksh.kontext.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class AuthorizationFailedException extends BusinessException {
    
    private static final String DEFAULT_MESSAGE = "Authorization failed - insufficient permissions";
    private static final AuthError AUTH_ERROR = AuthError.AUTHORIZATION_FAILED;
    
    public AuthorizationFailedException() {
        super(AUTH_ERROR, HttpStatus.FORBIDDEN);
    }
    
    public AuthorizationFailedException(String message) {
        super(message, AUTH_ERROR, HttpStatus.FORBIDDEN);
    }
}