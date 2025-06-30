package com.moksh.kontext.project.exception;

import com.moksh.kontext.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class ProjectAccessDeniedException extends BusinessException {
    
    private static final ProjectError PROJECT_ERROR = ProjectError.PROJECT_ACCESS_DENIED;
    
    public ProjectAccessDeniedException() {
        super(PROJECT_ERROR, HttpStatus.FORBIDDEN);
    }
    
    public ProjectAccessDeniedException(String message) {
        super(message, PROJECT_ERROR, HttpStatus.FORBIDDEN);
    }
    
    public static ProjectAccessDeniedException forUser(String userId, String projectId) {
        return new ProjectAccessDeniedException(String.format("User %s does not have access to project %s", userId, projectId));
    }
    
    public static ProjectAccessDeniedException forAction(String action, String projectId) {
        return new ProjectAccessDeniedException(String.format("Access denied for action '%s' on project %s", action, projectId));
    }
}