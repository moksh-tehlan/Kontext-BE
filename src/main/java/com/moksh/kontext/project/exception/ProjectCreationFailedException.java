package com.moksh.kontext.project.exception;

import com.moksh.kontext.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class ProjectCreationFailedException extends BusinessException {
    
    private static final ProjectError PROJECT_ERROR = ProjectError.PROJECT_CREATE_FAILED;
    
    public ProjectCreationFailedException() {
        super(PROJECT_ERROR, HttpStatus.BAD_REQUEST);
    }
    
    public ProjectCreationFailedException(String message) {
        super(message, PROJECT_ERROR, HttpStatus.BAD_REQUEST);
    }
    
    public static ProjectCreationFailedException withReason(String reason) {
        return new ProjectCreationFailedException(String.format("Failed to create project: %s", reason));
    }
}