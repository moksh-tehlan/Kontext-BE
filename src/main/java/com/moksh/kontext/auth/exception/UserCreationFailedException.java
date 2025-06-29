package com.moksh.kontext.auth.exception;

import com.moksh.kontext.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class UserCreationFailedException extends BusinessException {
    
    private static final String DEFAULT_MESSAGE = "Failed to create user account. Please try again";
    private static final AuthError AUTH_ERROR = AuthError.USER_CREATION_FAILED;
    
    public UserCreationFailedException() {
        super(AUTH_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    public UserCreationFailedException(String message) {
        super(message, AUTH_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    public UserCreationFailedException(String message, Throwable cause) {
        super(String.format("%s: %s", DEFAULT_MESSAGE, message), AUTH_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}