package com.moksh.kontext.user.exception;

import com.moksh.kontext.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class UserNotFoundException extends BusinessException {
    
    private static final UserError USER_ERROR = UserError.PROFILE_NOT_FOUND;
    
    public UserNotFoundException() {
        super(USER_ERROR, HttpStatus.NOT_FOUND);
    }
    
    public UserNotFoundException(String message) {
        super(message, USER_ERROR, HttpStatus.NOT_FOUND);
    }
    
    public static UserNotFoundException forId(String userId) {
        return new UserNotFoundException(String.format("User with ID %s not found", userId));
    }
    
    public static UserNotFoundException forEmail(String email) {
        return new UserNotFoundException(String.format("User with email %s not found", email));
    }
}