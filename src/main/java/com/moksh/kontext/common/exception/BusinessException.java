package com.moksh.kontext.common.exception;

import com.moksh.kontext.common.error.AppError;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BusinessException extends RuntimeException {
    
    private final HttpStatus httpStatus;
    private final String errorCode;
    private final Integer customStatusCode;
    
    public BusinessException(String message) {
        super(message);
        this.httpStatus = HttpStatus.BAD_REQUEST;
        this.errorCode = "BUSINESS_ERROR";
        this.customStatusCode = null;
    }
    
    public BusinessException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = "BUSINESS_ERROR";
        this.customStatusCode = null;
    }
    
    public BusinessException(String message, String errorCode, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
        this.customStatusCode = null;
    }
    
    public BusinessException(String message, String errorCode, HttpStatus httpStatus, Integer customStatusCode) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
        this.customStatusCode = customStatusCode;
    }
    
    // New constructor for AppError-based exceptions
    public BusinessException(String message, AppError appError, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = appError.getErrorCode();
        this.customStatusCode = appError.getStatusCode();
    }
    
    public BusinessException(AppError appError, HttpStatus httpStatus) {
        super(appError.getDefaultMessage());
        this.httpStatus = httpStatus;
        this.errorCode = appError.getErrorCode();
        this.customStatusCode = appError.getStatusCode();
    }
}