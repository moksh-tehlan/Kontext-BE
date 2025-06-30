package com.moksh.kontext.project.exception;

import com.moksh.kontext.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class ProjectDeleteFailedException extends BusinessException {
    
    private static final ProjectError PROJECT_ERROR = ProjectError.PROJECT_DELETE_FAILED;
    
    public ProjectDeleteFailedException() {
        super(PROJECT_ERROR, HttpStatus.BAD_REQUEST);
    }
    
    public ProjectDeleteFailedException(String message) {
        super(message, PROJECT_ERROR, HttpStatus.BAD_REQUEST);
    }
    
    public static ProjectDeleteFailedException forId(String projectId) {
        return new ProjectDeleteFailedException(String.format("Failed to delete project with ID %s", projectId));
    }
    
    public static ProjectDeleteFailedException withReason(String projectId, String reason) {
        return new ProjectDeleteFailedException(String.format("Failed to delete project %s: %s", projectId, reason));
    }
}