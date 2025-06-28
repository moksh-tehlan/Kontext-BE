package com.moksh.kontext.knowledge_processing.dto.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ContentProcessRequestEvent extends ContentProcessEvent {
    
    private String name;
    private String s3Key;
    private String s3Bucket;
    private String mimeType;
    private Long fileSize;
    private UUID projectId;
    private UUID userId;
    private String webUrl; // For web content
}