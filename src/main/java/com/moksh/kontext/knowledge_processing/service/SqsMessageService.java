package com.moksh.kontext.knowledge_processing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moksh.kontext.knowledge_processing.config.KnowledgeProcessingConfig;
import com.moksh.kontext.knowledge_processing.dto.event.ContentProcessEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import software.amazon.awssdk.services.sqs.model.SqsException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

@Service
@RequiredArgsConstructor
@Slf4j
public class SqsMessageService {

    private final SqsClient sqsClient;
    private final KnowledgeProcessingConfig knowledgeProcessingConfig;
    private final ObjectMapper objectMapper;

    @Retryable(value = {SqsException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public void sendContentProcessingRequest(ContentProcessEvent event) {
        try {
            String messageBody = objectMapper.writeValueAsString(event);
            
            SendMessageRequest request = SendMessageRequest.builder()
                    .queueUrl(knowledgeProcessingConfig.getProcessQueueUrl())
                    .messageBody(messageBody)
                    .build();

            SendMessageResponse response = sqsClient.sendMessage(request);
            
            log.info("Successfully sent content processing request to SQS. MessageId: {}, EventId: {}, ContentType: {}", 
                    response.messageId(), event.getEventId(), event.getContentType());
                    
        } catch (SqsException e) {
            log.error("Failed to send content processing request to SQS. EventId: {}, ContentType: {}, Error: {}", 
                    event.getEventId(), event.getContentType(), e.getMessage(), e);
            throw new RuntimeException("Failed to send message to SQS", e);
        } catch (Exception e) {
            log.error("Unexpected error while sending content processing request. EventId: {}, ContentType: {}, Error: {}", 
                    event.getEventId(), event.getContentType(), e.getMessage(), e);
            throw new RuntimeException("Unexpected error while sending message", e);
        }
    }
}