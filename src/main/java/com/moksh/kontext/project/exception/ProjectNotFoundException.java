package com.moksh.kontext.project.exception;

import com.moksh.kontext.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class ProjectNotFoundException extends BusinessException {
    
    private static final ProjectError PROJECT_ERROR = ProjectError.PROJECT_NOT_FOUND;
    
    public ProjectNotFoundException() {
        super(PROJECT_ERROR, HttpStatus.NOT_FOUND);
    }
    
    public ProjectNotFoundException(String message) {
        super(message, PROJECT_ERROR, HttpStatus.NOT_FOUND);
    }
    
    public static ProjectNotFoundException forId(String projectId) {
        return new ProjectNotFoundException(String.format("Project with ID %s not found", projectId));
    }
    
    public static ProjectNotFoundException forUserAndId(String userId, String projectId) {
        return new ProjectNotFoundException(String.format("Project with ID %s not found for user %s", projectId, userId));
    }
}