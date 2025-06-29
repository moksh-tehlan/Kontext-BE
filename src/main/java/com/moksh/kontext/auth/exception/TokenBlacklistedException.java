package com.moksh.kontext.auth.exception;

import com.moksh.kontext.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class TokenBlacklistedException extends BusinessException {
    
    private static final AuthError AUTH_ERROR = AuthError.TOKEN_BLACKLISTED;
    
    public TokenBlacklistedException() {
        super(AUTH_ERROR, HttpStatus.UNAUTHORIZED);
    }
    
    public TokenBlacklistedException(String message) {
        super(message, AUTH_ERROR, HttpStatus.UNAUTHORIZED);
    }
}