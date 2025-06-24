package com.moksh.kontext.common.response;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import javax.validation.constraints.NotNull;

@RestControllerAdvice
public class GlobalResponseBodyAdvice implements ResponseBodyAdvice<Object> {
    
    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // Skip wrapping if return type is already ApiResponse
        if (returnType.getParameterType().equals(ApiResponse.class)) {
            return false;
        }
        
        // Skip wrapping for error responses (handled by GlobalExceptionHandler)
        String methodName = returnType.getMethod() != null ? returnType.getMethod().getName() : "";
        if (methodName.contains("error") || methodName.contains("exception")) {
            return false;
        }
        
        // Skip wrapping for actuator endpoints
        String declaringClassName = returnType.getDeclaringClass().getName();
        if (declaringClassName.contains("actuator") || declaringClassName.contains("health")) {
            return false;
        }
        
        return true;
    }
    
    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        
        // If body is null, wrap it in success response
        if (body == null) {
            return ApiResponse.success(null, "Operation completed successfully");
        }
        
        // If body is already ApiResponse, return as is
        if (body instanceof ApiResponse) {
            return body;
        }
        
        // For String responses (common with @ResponseBody), we need special handling
        if (body instanceof String) {
            return ApiResponse.success(body);
        }
        
        // Wrap the response in ApiResponse
        return ApiResponse.success(body);
    }
}