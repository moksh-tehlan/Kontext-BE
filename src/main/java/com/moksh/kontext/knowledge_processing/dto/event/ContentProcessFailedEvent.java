package com.moksh.kontext.knowledge_processing.dto.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ContentProcessFailedEvent extends ContentProcessEvent {
    
    private String errorMessage;
    private String errorCode;
    private String stackTrace;
    private Integer retryCount;
    private String failedStep;
}