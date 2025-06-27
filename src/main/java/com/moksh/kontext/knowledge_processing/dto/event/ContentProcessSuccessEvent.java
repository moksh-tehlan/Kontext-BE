package com.moksh.kontext.knowledge_processing.dto.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.ai.document.Document;

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ContentProcessSuccessEvent extends ContentProcessEvent {
    
    private String message;
    private Long processingTimeMs;
    private Integer chunkCount;
    private String s3BucketName;
    private String s3Key; // S3 key where the JSON of chunks is stored
    private List<Document> documents; // Spring AI Document objects - will be populated from S3
}