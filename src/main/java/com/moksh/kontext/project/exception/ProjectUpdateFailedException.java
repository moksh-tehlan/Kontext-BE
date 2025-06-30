package com.moksh.kontext.project.exception;

import com.moksh.kontext.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class ProjectUpdateFailedException extends BusinessException {
    
    private static final ProjectError PROJECT_ERROR = ProjectError.PROJECT_UPDATE_FAILED;
    
    public ProjectUpdateFailedException() {
        super(PROJECT_ERROR, HttpStatus.BAD_REQUEST);
    }
    
    public ProjectUpdateFailedException(String message) {
        super(message, PROJECT_ERROR, HttpStatus.BAD_REQUEST);
    }
    
    public static ProjectUpdateFailedException forId(String projectId) {
        return new ProjectUpdateFailedException(String.format("Failed to update project with ID %s", projectId));
    }
    
    public static ProjectUpdateFailedException withReason(String projectId, String reason) {
        return new ProjectUpdateFailedException(String.format("Failed to update project %s: %s", projectId, reason));
    }
}