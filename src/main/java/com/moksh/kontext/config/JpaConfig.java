package com.moksh.kontext.config;

import com.moksh.kontext.common.util.SecurityContextUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            String userEmail = SecurityContextUtil.getCurrentUserEmail();
            return Optional.of(userEmail != null ? userEmail : "system");
        };
    }
}