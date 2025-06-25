package com.moksh.kontext.common.exception;

public class InvalidWebUrlException extends RuntimeException {
    
    public InvalidWebUrlException(String message) {
        super(message);
    }
    
    public InvalidWebUrlException(String message, Throwable cause) {
        super(message, cause);
    }
}