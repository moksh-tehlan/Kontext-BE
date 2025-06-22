package com.moksh.kontext.common.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public abstract class BaseDto {
    private Long id;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
    private Long version;
    private Boolean isActive;
}