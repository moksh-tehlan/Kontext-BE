package com.moksh.kontext.user.exception;

import com.moksh.kontext.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class DuplicateEmailException extends BusinessException {
    
    private static final UserError USER_ERROR = UserError.DUPLICATE_EMAIL;
    
    public DuplicateEmailException() {
        super(USER_ERROR, HttpStatus.CONFLICT);
    }
    
    public DuplicateEmailException(String message) {
        super(message, USER_ERROR, HttpStatus.CONFLICT);
    }
    
    public static DuplicateEmailException forEmail(String email) {
        return new DuplicateEmailException(String.format("Email address %s is already in use", email));
    }
}