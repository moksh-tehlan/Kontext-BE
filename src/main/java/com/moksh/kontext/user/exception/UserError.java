package com.moksh.kontext.user.exception;

import com.moksh.kontext.common.error.BaseAppError;

public class UserError extends BaseAppError {
    
    private static final String MODULE = "USER";
    
    // Private constructor for creating user error instances
    private UserError(String errorCode, int statusCode, String defaultMessage) {
        super(errorCode, statusCode, MODULE, defaultMessage);
    }
    
    // Profile Related Errors (5000-5099)
    public static final UserError PROFILE_NOT_FOUND = new UserError("USER_PROFILE_NOT_FOUND", 5001, "User profile not found");
    public static final UserError PROFILE_UPDATE_FAILED = new UserError("USER_PROFILE_UPDATE_FAILED", 5002, "Failed to update user profile");
    public static final UserError INVALID_PROFILE_DATA = new UserError("USER_INVALID_PROFILE_DATA", 5003, "Invalid profile data provided");
    
    // Permission Related Errors (5100-5199)
    public static final UserError INSUFFICIENT_PERMISSIONS = new UserError("USER_INSUFFICIENT_PERMISSIONS", 5101, "Insufficient permissions to perform this action");
    public static final UserError ROLE_ASSIGNMENT_FAILED = new UserError("USER_ROLE_ASSIGNMENT_FAILED", 5102, "Failed to assign role to user");
    
    // Account Related Errors (5200-5299)
    public static final UserError ACCOUNT_SUSPENDED = new UserError("USER_ACCOUNT_SUSPENDED", 5201, "User account has been suspended");
    public static final UserError DUPLICATE_EMAIL = new UserError("USER_DUPLICATE_EMAIL", 5202, "Email address is already in use");
    public static final UserError INVALID_USER_STATUS = new UserError("USER_INVALID_STATUS", 5203, "Invalid user status");
}