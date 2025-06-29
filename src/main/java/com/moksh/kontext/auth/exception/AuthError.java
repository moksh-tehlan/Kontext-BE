package com.moksh.kontext.auth.exception;

import com.moksh.kontext.common.error.BaseAppError;

public class AuthError extends BaseAppError {
    
    private static final String MODULE = "AUTH";
    
    // Private constructor for creating auth error instances
    private AuthError(String errorCode, int statusCode, String defaultMessage) {
        super(errorCode, statusCode, MODULE, defaultMessage);
    }
    
    private AuthError(String errorCode, int statusCode) {
        super(errorCode, statusCode, MODULE);
    }
    
    // OTP Related Errors (4100-4199)
    public static final AuthError OTP_MISMATCH = new AuthError("AUTH_OTP_MISMATCH", 4101, "The OTP provided does not match");
    public static final AuthError OTP_EXPIRED = new AuthError("AUTH_OTP_EXPIRED", 4102, "The OTP has expired. Please request a new one");
    public static final AuthError OTP_NOT_FOUND = new AuthError("AUTH_OTP_NOT_FOUND", 4103, "No OTP found for this email. Please request a new OTP");
    
    // Account Related Errors (4200-4299)
    public static final AuthError ACCOUNT_DEACTIVATED = new AuthError("AUTH_ACCOUNT_DEACTIVATED", 4201, "Your account has been deactivated. Please contact support");
    public static final AuthError USER_CREATION_FAILED = new AuthError("AUTH_USER_CREATION_FAILED", 4202, "Failed to create user account. Please try again");
    
    // Token Related Errors (4300-4399)
    public static final AuthError INVALID_REFRESH_TOKEN = new AuthError("AUTH_INVALID_REFRESH_TOKEN", 4301, "Invalid or expired refresh token. Please login again");
    public static final AuthError GOOGLE_TOKEN_INVALID = new AuthError("AUTH_GOOGLE_TOKEN_INVALID", 4302, "Failed to verify Google ID token. Please try again");
    public static final AuthError JWT_TOKEN_EXPIRED = new AuthError("AUTH_JWT_TOKEN_EXPIRED", 4303, "JWT token has expired. Please login again");
    public static final AuthError JWT_TOKEN_MALFORMED = new AuthError("AUTH_JWT_TOKEN_MALFORMED", 4304, "JWT token is malformed or invalid");
    public static final AuthError JWT_TOKEN_MISSING = new AuthError("AUTH_JWT_TOKEN_MISSING", 4305, "JWT token is missing from request");
    public static final AuthError JWT_SIGNATURE_INVALID = new AuthError("AUTH_JWT_SIGNATURE_INVALID", 4306, "JWT token signature is invalid");
    public static final AuthError ACCESS_TOKEN_INVALID = new AuthError("AUTH_ACCESS_TOKEN_INVALID", 4307, "Invalid access token provided");
    public static final AuthError TOKEN_BLACKLISTED = new AuthError("AUTH_TOKEN_BLACKLISTED", 4308, "Token has been blacklisted. Please login again");
    
    // General Auth Errors (4000-4099)
    public static final AuthError AUTHENTICATION_FAILED = new AuthError("AUTH_AUTHENTICATION_FAILED", 4001, "Authentication failed");
    public static final AuthError AUTHORIZATION_FAILED = new AuthError("AUTH_AUTHORIZATION_FAILED", 4002, "Authorization failed");
}