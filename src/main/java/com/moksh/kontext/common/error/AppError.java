package com.moksh.kontext.common.error;

/**
 * Base interface for all application errors.
 * This ensures consistency across all error types in the application.
 * 
 * Error Code Format: {MODULE}_{ERROR_TYPE}
 * Status Code Ranges:
 * - Auth Errors: 4000-4999
 * - User Errors: 5000-5999  
 * - Project Errors: 6000-6999
 * - Chat Errors: 7000-7999
 * - General Errors: 8000-8999
 */
public interface AppError {
    
    /**
     * Gets the error code that identifies this specific error type
     * @return error code string (e.g., "AUTH_OTP_MISMATCH")
     */
    String getErrorCode();
    
    /**
     * Gets the custom status code for frontend mapping
     * @return custom status code integer (e.g., 4101)
     */
    int getStatusCode();
    
    /**
     * Gets the module/domain this error belongs to
     * @return module name (e.g., "AUTH", "USER", "PROJECT")
     */
    String getModule();
    
    /**
     * Gets a default message for this error type
     * @return default error message
     */
    default String getDefaultMessage() {
        return "An error occurred";
    }
}