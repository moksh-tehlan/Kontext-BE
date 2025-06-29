package com.moksh.kontext.user.exception;

import com.moksh.kontext.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class ProfileUpdateFailedException extends BusinessException {
    
    private static final UserError USER_ERROR = UserError.PROFILE_UPDATE_FAILED;
    
    public ProfileUpdateFailedException() {
        super(USER_ERROR, HttpStatus.BAD_REQUEST);
    }
    
    public ProfileUpdateFailedException(String message) {
        super(message, USER_ERROR, HttpStatus.BAD_REQUEST);
    }
    
    public static ProfileUpdateFailedException forUser(Long userId) {
        return new ProfileUpdateFailedException(String.format("Failed to update profile for user ID %d", userId));
    }
}