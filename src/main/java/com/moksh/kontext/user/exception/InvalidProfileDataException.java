package com.moksh.kontext.user.exception;

import com.moksh.kontext.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class InvalidProfileDataException extends BusinessException {
    
    private static final UserError USER_ERROR = UserError.INVALID_PROFILE_DATA;
    
    public InvalidProfileDataException() {
        super(USER_ERROR, HttpStatus.BAD_REQUEST);
    }
    
    public InvalidProfileDataException(String message) {
        super(message, USER_ERROR, HttpStatus.BAD_REQUEST);
    }
    
    public static InvalidProfileDataException forField(String fieldName) {
        return new InvalidProfileDataException(String.format("Invalid data provided for field: %s", fieldName));
    }
}