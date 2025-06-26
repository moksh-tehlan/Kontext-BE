package com.moksh.kontext.google.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "google.oauth2")
@Getter
@Setter
public class GoogleConfig {
    
    private String clientId;
}