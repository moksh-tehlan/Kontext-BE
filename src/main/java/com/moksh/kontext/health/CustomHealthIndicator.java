package com.moksh.kontext.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
public class CustomHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        try {
            Map<String, Object> details = new HashMap<>();
            details.put("application", "Kontext");
            details.put("status", "UP");
            details.put("timestamp", LocalDateTime.now());
            details.put("version", "1.0.0");
            details.put("description", "Kontext application health check");
            
            long freeMemory = Runtime.getRuntime().freeMemory();
            long totalMemory = Runtime.getRuntime().totalMemory();
            long usedMemory = totalMemory - freeMemory;
            
            Map<String, Object> memoryInfo = new HashMap<>();
            memoryInfo.put("free", freeMemory / (1024 * 1024) + " MB");
            memoryInfo.put("total", totalMemory / (1024 * 1024) + " MB");
            memoryInfo.put("used", usedMemory / (1024 * 1024) + " MB");
            
            details.put("memory", memoryInfo);
            
            return Health.up()
                    .withDetails(details)
                    .build();
                    
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}