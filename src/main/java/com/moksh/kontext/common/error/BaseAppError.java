package com.moksh.kontext.common.error;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Abstract base class for application errors.
 * Provides common implementation for AppError interface.
 */
@Getter
@AllArgsConstructor
public abstract class BaseAppError implements AppError {
    
    private final String errorCode;
    private final int statusCode;
    private final String module;
    private final String defaultMessage;
    
    /**
     * Constructor without default message
     */
    protected BaseAppError(String errorCode, int statusCode, String module) {
        this.errorCode = errorCode;
        this.statusCode = statusCode;
        this.module = module;
        this.defaultMessage = "An error occurred in " + module + " module";
    }
    
    @Override
    public String toString() {
        return String.format("%s[%s:%d] - %s", module, errorCode, statusCode, defaultMessage);
    }
}