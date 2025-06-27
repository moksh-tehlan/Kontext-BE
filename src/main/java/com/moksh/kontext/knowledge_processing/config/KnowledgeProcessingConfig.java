package com.moksh.kontext.knowledge_processing.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class KnowledgeProcessingConfig {

    // Queue for sending requests TO Python (process.queue)
    @Value("${aws.sqs.process-queue-url}")
    private String processQueueUrl;

    // Queue for receiving responses FROM Python (processing-queue)
    @Value("${aws.sqs.processing-queue-url}")
    private String processingQueueUrl;
}