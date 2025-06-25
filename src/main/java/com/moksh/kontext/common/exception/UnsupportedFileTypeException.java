package com.moksh.kontext.common.exception;

public class UnsupportedFileTypeException extends RuntimeException {
    
    public UnsupportedFileTypeException(String message) {
        super(message);
    }
    
    public UnsupportedFileTypeException(String message, Throwable cause) {
        super(message, cause);
    }
}