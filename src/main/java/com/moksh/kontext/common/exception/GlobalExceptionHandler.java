package com.moksh.kontext.common.exception;

import com.moksh.kontext.auth.exception.*;
import com.moksh.kontext.user.exception.*;
import com.moksh.kontext.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusinessException(
            BusinessException ex, HttpServletRequest request) {
        log.warn("Business exception: {}", ex.getMessage());
        
        // Use custom status code if available, otherwise fall back to HTTP status
        int statusCode = ex.getCustomStatusCode() != null ? ex.getCustomStatusCode() : ex.getHttpStatus().value();
        
        ApiResponse<Object> response = ApiResponse.error(
                ex.getMessage(),
                ex.getErrorCode(),
                request.getRequestURI(),
                statusCode
        );
        
        return ResponseEntity.status(ex.getHttpStatus()).body(response);
    }

    // Auth-specific Exception Handlers
    @ExceptionHandler(OtpDoesntMatchException.class)
    public ResponseEntity<ApiResponse<Object>> handleOtpDoesntMatchException(
            OtpDoesntMatchException ex, HttpServletRequest request) {
        log.warn("OTP mismatch: {}", ex.getMessage());
        
        ApiResponse<Object> response = ApiResponse.error(
                ex.getMessage(),
                ex.getErrorCode(),
                request.getRequestURI(),
                ex.getCustomStatusCode()
        );
        
        return ResponseEntity.status(ex.getHttpStatus()).body(response);
    }

    @ExceptionHandler(OtpExpiredException.class)
    public ResponseEntity<ApiResponse<Object>> handleOtpExpiredException(
            OtpExpiredException ex, HttpServletRequest request) {
        log.warn("OTP expired: {}", ex.getMessage());
        
        ApiResponse<Object> response = ApiResponse.error(
                ex.getMessage(),
                ex.getErrorCode(),
                request.getRequestURI(),
                ex.getHttpStatus().value()
        );
        
        return ResponseEntity.status(ex.getHttpStatus()).body(response);
    }

    @ExceptionHandler(OtpNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleOtpNotFoundException(
            OtpNotFoundException ex, HttpServletRequest request) {
        log.warn("OTP not found: {}", ex.getMessage());
        
        ApiResponse<Object> response = ApiResponse.error(
                ex.getMessage(),
                ex.getErrorCode(),
                request.getRequestURI(),
                ex.getCustomStatusCode()
        );
        
        return ResponseEntity.status(ex.getHttpStatus()).body(response);
    }

    @ExceptionHandler(UserAccountDeactivatedException.class)
    public ResponseEntity<ApiResponse<Object>> handleUserAccountDeactivatedException(
            UserAccountDeactivatedException ex, HttpServletRequest request) {
        log.warn("User account deactivated: {}", ex.getMessage());
        
        ApiResponse<Object> response = ApiResponse.error(
                ex.getMessage(),
                ex.getErrorCode(),
                request.getRequestURI(),
                ex.getHttpStatus().value()
        );
        
        return ResponseEntity.status(ex.getHttpStatus()).body(response);
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidRefreshTokenException(
            InvalidRefreshTokenException ex, HttpServletRequest request) {
        log.warn("Invalid refresh token: {}", ex.getMessage());
        
        ApiResponse<Object> response = ApiResponse.error(
                ex.getMessage(),
                ex.getErrorCode(),
                request.getRequestURI(),
                ex.getHttpStatus().value()
        );
        
        return ResponseEntity.status(ex.getHttpStatus()).body(response);
    }

    @ExceptionHandler(GoogleTokenVerificationException.class)
    public ResponseEntity<ApiResponse<Object>> handleGoogleTokenVerificationException(
            GoogleTokenVerificationException ex, HttpServletRequest request) {
        log.warn("Google token verification failed: {}", ex.getMessage());
        
        ApiResponse<Object> response = ApiResponse.error(
                ex.getMessage(),
                ex.getErrorCode(),
                request.getRequestURI(),
                ex.getHttpStatus().value()
        );
        
        return ResponseEntity.status(ex.getHttpStatus()).body(response);
    }

    @ExceptionHandler(UserCreationFailedException.class)
    public ResponseEntity<ApiResponse<Object>> handleUserCreationFailedException(
            UserCreationFailedException ex, HttpServletRequest request) {
        log.error("User creation failed: {}", ex.getMessage());
        
        ApiResponse<Object> response = ApiResponse.error(
                ex.getMessage(),
                ex.getErrorCode(),
                request.getRequestURI(),
                ex.getCustomStatusCode()
        );
        
        return ResponseEntity.status(ex.getHttpStatus()).body(response);
    }

    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAuthenticationFailedException(
            AuthenticationFailedException ex, HttpServletRequest request) {
        log.warn("Authentication failed: {}", ex.getMessage());
        
        ApiResponse<Object> response = ApiResponse.error(
                ex.getMessage(),
                ex.getErrorCode(),
                request.getRequestURI(),
                ex.getCustomStatusCode()
        );
        
        return ResponseEntity.status(ex.getHttpStatus()).body(response);
    }

    @ExceptionHandler(AuthorizationFailedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAuthorizationFailedException(
            AuthorizationFailedException ex, HttpServletRequest request) {
        log.warn("Authorization failed: {}", ex.getMessage());
        
        ApiResponse<Object> response = ApiResponse.error(
                ex.getMessage(),
                ex.getErrorCode(),
                request.getRequestURI(),
                ex.getCustomStatusCode()
        );
        
        return ResponseEntity.status(ex.getHttpStatus()).body(response);
    }

    // User-specific Exception Handlers
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleUserNotFoundException(
            UserNotFoundException ex, HttpServletRequest request) {
        log.warn("User not found: {}", ex.getMessage());
        
        ApiResponse<Object> response = ApiResponse.error(
                ex.getMessage(),
                ex.getErrorCode(),
                request.getRequestURI(),
                ex.getCustomStatusCode()
        );
        
        return ResponseEntity.status(ex.getHttpStatus()).body(response);
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ApiResponse<Object>> handleDuplicateEmailException(
            DuplicateEmailException ex, HttpServletRequest request) {
        log.warn("Duplicate email: {}", ex.getMessage());
        
        ApiResponse<Object> response = ApiResponse.error(
                ex.getMessage(),
                ex.getErrorCode(),
                request.getRequestURI(),
                ex.getCustomStatusCode()
        );
        
        return ResponseEntity.status(ex.getHttpStatus()).body(response);
    }

    @ExceptionHandler(InvalidProfileDataException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidProfileDataException(
            InvalidProfileDataException ex, HttpServletRequest request) {
        log.warn("Invalid profile data: {}", ex.getMessage());
        
        ApiResponse<Object> response = ApiResponse.error(
                ex.getMessage(),
                ex.getErrorCode(),
                request.getRequestURI(),
                ex.getCustomStatusCode()
        );
        
        return ResponseEntity.status(ex.getHttpStatus()).body(response);
    }

    @ExceptionHandler(ProfileUpdateFailedException.class)
    public ResponseEntity<ApiResponse<Object>> handleProfileUpdateFailedException(
            ProfileUpdateFailedException ex, HttpServletRequest request) {
        log.warn("Profile update failed: {}", ex.getMessage());
        
        ApiResponse<Object> response = ApiResponse.error(
                ex.getMessage(),
                ex.getErrorCode(),
                request.getRequestURI(),
                ex.getCustomStatusCode()
        );
        
        return ResponseEntity.status(ex.getHttpStatus()).body(response);
    }

    @ExceptionHandler(InsufficientPermissionsException.class)
    public ResponseEntity<ApiResponse<Object>> handleInsufficientPermissionsException(
            InsufficientPermissionsException ex, HttpServletRequest request) {
        log.warn("Insufficient permissions: {}", ex.getMessage());
        
        ApiResponse<Object> response = ApiResponse.error(
                ex.getMessage(),
                ex.getErrorCode(),
                request.getRequestURI(),
                ex.getCustomStatusCode()
        );
        
        return ResponseEntity.status(ex.getHttpStatus()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        log.warn("Validation error: {}", ex.getMessage());
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ApiResponse<Map<String, String>> response = ApiResponse.error(
                "Validation failed",
                "VALIDATION_ERROR",
                request.getRequestURI(),
                HttpStatus.BAD_REQUEST.value()
        );
        response.setData(errors);
        
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleConstraintViolationException(
            ConstraintViolationException ex, HttpServletRequest request) {
        log.warn("Constraint violation: {}", ex.getMessage());
        
        Map<String, String> errors = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        ConstraintViolation::getMessage
                ));

        ApiResponse<Map<String, String>> response = ApiResponse.error(
                "Constraint validation failed",
                "CONSTRAINT_VIOLATION",
                request.getRequestURI(),
                HttpStatus.BAD_REQUEST.value()
        );
        response.setData(errors);
        
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("Invalid JSON format: {}", ex.getMessage());
        
        ApiResponse<Object> response = ApiResponse.error(
                "Invalid request body format",
                "INVALID_JSON",
                request.getRequestURI(),
                HttpStatus.BAD_REQUEST.value()
        );
        
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        log.warn("Type mismatch: {}", ex.getMessage());
        
        String message = String.format("Invalid value '%s' for parameter '%s'", 
                ex.getValue(), ex.getName());
        
        ApiResponse<Object> response = ApiResponse.error(
                message,
                "TYPE_MISMATCH",
                request.getRequestURI(),
                HttpStatus.BAD_REQUEST.value()
        );
        
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Object>> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex, HttpServletRequest request) {
        log.warn("Missing parameter: {}", ex.getMessage());
        
        String message = String.format("Required parameter '%s' is missing", ex.getParameterName());
        
        ApiResponse<Object> response = ApiResponse.error(
                message,
                "MISSING_PARAMETER",
                request.getRequestURI(),
                HttpStatus.BAD_REQUEST.value()
        );
        
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Object>> handleHttpRequestMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        log.warn("Method not supported: {}", ex.getMessage());
        
        String message = String.format("HTTP method '%s' is not supported for this endpoint", 
                ex.getMethod());
        
        ApiResponse<Object> response = ApiResponse.error(
                message,
                "METHOD_NOT_SUPPORTED",
                request.getRequestURI(),
                HttpStatus.METHOD_NOT_ALLOWED.value()
        );
        
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNoHandlerFoundException(
            NoHandlerFoundException ex, HttpServletRequest request) {
        log.warn("No handler found: {}", ex.getMessage());
        
        ApiResponse<Object> response = ApiResponse.error(
                "Endpoint not found",
                "ENDPOINT_NOT_FOUND",
                request.getRequestURI(),
                HttpStatus.NOT_FOUND.value()
        );
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDeniedException(
            AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied: {}", ex.getMessage());
        
        ApiResponse<Object> response = ApiResponse.error(
                "Access denied",
                "ACCESS_DENIED",
                request.getRequestURI(),
                HttpStatus.FORBIDDEN.value()
        );
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    // File Upload Exceptions
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Object>> handleMaxUploadSizeExceeded(
            MaxUploadSizeExceededException ex, HttpServletRequest request) {
        log.warn("File size exceeded: {}", ex.getMessage());
        
        String message = "File size exceeds the maximum allowed limit of 50MB";
        
        ApiResponse<Object> response = ApiResponse.error(
                message,
                "FILE_SIZE_EXCEEDED",
                request.getRequestURI(),
                HttpStatus.BAD_REQUEST.value()
        );
        
        return ResponseEntity.badRequest().body(response);
    }

    // Knowledge Management Exceptions
    @ExceptionHandler(UnsupportedFileTypeException.class)
    public ResponseEntity<ApiResponse<Object>> handleUnsupportedFileTypeException(
            UnsupportedFileTypeException ex, HttpServletRequest request) {
        log.warn("Unsupported file type: {}", ex.getMessage());
        
        ApiResponse<Object> response = ApiResponse.error(
                ex.getMessage(),
                "UNSUPPORTED_FILE_TYPE",
                request.getRequestURI(),
                HttpStatus.BAD_REQUEST.value()
        );
        
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(InvalidWebUrlException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidWebUrlException(
            InvalidWebUrlException ex, HttpServletRequest request) {
        log.warn("Invalid web URL: {}", ex.getMessage());
        
        ApiResponse<Object> response = ApiResponse.error(
                ex.getMessage(),
                "INVALID_WEB_URL",
                request.getRequestURI(),
                HttpStatus.BAD_REQUEST.value()
        );
        
        return ResponseEntity.badRequest().body(response);
    }

    // AWS S3 Exceptions
    @ExceptionHandler(S3UploadException.class)
    public ResponseEntity<ApiResponse<Object>> handleS3UploadException(
            S3UploadException ex, HttpServletRequest request) {
        log.error("S3 upload failed: {}", ex.getMessage());
        
        ApiResponse<Object> response = ApiResponse.error(
                "File upload failed. Please try again.",
                "S3_UPLOAD_ERROR",
                request.getRequestURI(),
                HttpStatus.INTERNAL_SERVER_ERROR.value()
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(S3DeleteException.class)
    public ResponseEntity<ApiResponse<Object>> handleS3DeleteException(
            S3DeleteException ex, HttpServletRequest request) {
        log.error("S3 delete failed: {}", ex.getMessage());
        
        ApiResponse<Object> response = ApiResponse.error(
                "File deletion failed. Please try again.",
                "S3_DELETE_ERROR",
                request.getRequestURI(),
                HttpStatus.INTERNAL_SERVER_ERROR.value()
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGenericException(
            Exception ex, HttpServletRequest request) {
        log.error("Unexpected error occurred", ex);
        
        ApiResponse<Object> response = ApiResponse.error(
                "An unexpected error occurred",
                "INTERNAL_SERVER_ERROR",
                request.getRequestURI(),
                HttpStatus.INTERNAL_SERVER_ERROR.value()
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}