package com.moksh.kontext.user.exception;

import com.moksh.kontext.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class InsufficientPermissionsException extends BusinessException {
    
    private static final UserError USER_ERROR = UserError.INSUFFICIENT_PERMISSIONS;
    
    public InsufficientPermissionsException() {
        super(USER_ERROR, HttpStatus.FORBIDDEN);
    }
    
    public InsufficientPermissionsException(String message) {
        super(message, USER_ERROR, HttpStatus.FORBIDDEN);
    }
    
    public static InsufficientPermissionsException forAction(String action) {
        return new InsufficientPermissionsException(String.format("Insufficient permissions to %s", action));
    }
}