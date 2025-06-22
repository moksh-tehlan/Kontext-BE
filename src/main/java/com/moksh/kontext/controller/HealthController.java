package com.moksh.kontext.controller;

import com.moksh.kontext.common.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/health")
public class HealthController {

    @GetMapping
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> healthData = new HashMap<>();
        healthData.put("status", "UP");
        healthData.put("timestamp", LocalDateTime.now());
        healthData.put("application", "Kontext");
        healthData.put("version", "1.0.0");
        
        return ApiResponse.success(healthData, "Application is healthy");
    }
    
    @GetMapping("/ping")
    public ApiResponse<String> ping() {
        return ApiResponse.success("pong", "Ping successful");
    }
}