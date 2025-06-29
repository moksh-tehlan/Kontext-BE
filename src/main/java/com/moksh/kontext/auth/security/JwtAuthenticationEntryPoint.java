package com.moksh.kontext.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moksh.kontext.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        
        log.warn("Unauthorized error: {} for request: {}", authException.getMessage(), request.getRequestURI());
        
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        
        // Check if there's a specific JWT error attribute set by the filter
        String jwtError = (String) request.getAttribute("jwt.error");
        Integer jwtErrorCode = (Integer) request.getAttribute("jwt.error.code");
        
        String errorMessage;
        Integer errorCode;
        
        if (jwtError != null && jwtErrorCode != null) {
            errorMessage = jwtError;
            errorCode = jwtErrorCode;
        } else {
            // Default authentication error
            errorMessage = "Authentication required. Please provide a valid JWT token";
            errorCode = 4001; // AUTH_AUTHENTICATION_FAILED
        }
        
        ApiResponse<Object> apiResponse = ApiResponse.error(
                errorMessage,
                "AUTH_AUTHENTICATION_FAILED",
                request.getRequestURI(),
                errorCode
        );
        
        objectMapper.writeValue(response.getOutputStream(), apiResponse);
    }
}