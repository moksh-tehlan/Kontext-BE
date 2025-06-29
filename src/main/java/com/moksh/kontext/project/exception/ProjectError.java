package com.moksh.kontext.project.exception;

import com.moksh.kontext.common.error.BaseAppError;

public class ProjectError extends BaseAppError {
    
    private static final String MODULE = "PROJECT";
    
    // Private constructor for creating project error instances
    private ProjectError(String errorCode, int statusCode, String defaultMessage) {
        super(errorCode, statusCode, MODULE, defaultMessage);
    }
    
    // Project Management Errors (6000-6099)
    public static final ProjectError PROJECT_NOT_FOUND = new ProjectError("PROJECT_NOT_FOUND", 6001, "Project not found");
    public static final ProjectError PROJECT_CREATE_FAILED = new ProjectError("PROJECT_CREATE_FAILED", 6002, "Failed to create project");
    public static final ProjectError PROJECT_UPDATE_FAILED = new ProjectError("PROJECT_UPDATE_FAILED", 6003, "Failed to update project");
    public static final ProjectError PROJECT_DELETE_FAILED = new ProjectError("PROJECT_DELETE_FAILED", 6004, "Failed to delete project");
    
    // Project Access Errors (6100-6199)
    public static final ProjectError PROJECT_ACCESS_DENIED = new ProjectError("PROJECT_ACCESS_DENIED", 6101, "Access denied to project");
    public static final ProjectError PROJECT_MEMBER_NOT_FOUND = new ProjectError("PROJECT_MEMBER_NOT_FOUND", 6102, "Project member not found");
    public static final ProjectError PROJECT_INVITATION_FAILED = new ProjectError("PROJECT_INVITATION_FAILED", 6103, "Failed to send project invitation");
    
    // Project Status Errors (6200-6299)
    public static final ProjectError PROJECT_ALREADY_ARCHIVED = new ProjectError("PROJECT_ALREADY_ARCHIVED", 6201, "Project is already archived");
    public static final ProjectError PROJECT_INVALID_STATUS = new ProjectError("PROJECT_INVALID_STATUS", 6202, "Invalid project status");
}