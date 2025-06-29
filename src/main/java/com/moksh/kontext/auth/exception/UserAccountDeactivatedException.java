package com.moksh.kontext.auth.exception;

import com.moksh.kontext.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class UserAccountDeactivatedException extends BusinessException {
    
    private static final String DEFAULT_MESSAGE = "Your account has been deactivated. Please contact support";
    private static final AuthError AUTH_ERROR = AuthError.ACCOUNT_DEACTIVATED;
    
    public UserAccountDeactivatedException() {
        super(AUTH_ERROR, HttpStatus.FORBIDDEN);
    }
    
    public UserAccountDeactivatedException(String message) {
        super(message, AUTH_ERROR, HttpStatus.FORBIDDEN);
    }
}