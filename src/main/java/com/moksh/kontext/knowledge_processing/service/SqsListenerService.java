package com.moksh.kontext.knowledge_processing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moksh.kontext.knowledge_processing.config.KnowledgeProcessingConfig;
import com.moksh.kontext.knowledge_processing.dto.event.ContentProcessEvent;
import com.moksh.kontext.knowledge_processing.dto.event.ContentProcessFailedEvent;
import com.moksh.kontext.knowledge_processing.dto.event.ContentProcessSuccessEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.ai.document.Document;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SqsListenerService {

    private final SqsClient sqsClient;
    private final S3Client s3Client;
    private final KnowledgeProcessingConfig knowledgeProcessingConfig;
    private final ObjectMapper objectMapper;
    private final ContentProcessingStatusService contentProcessingStatusService;

    @Scheduled(fixedDelay = 5000)
    @Async
    public void pollContentProcessingStatusMessages() {
        try {
            ReceiveMessageRequest request = ReceiveMessageRequest.builder()
                    .queueUrl(knowledgeProcessingConfig.getProcessingQueueUrl())
                    .maxNumberOfMessages(10)
                    .waitTimeSeconds(20)
                    .build();

            ReceiveMessageResponse response = sqsClient.receiveMessage(request);
            List<Message> messages = response.messages();

            for (Message message : messages) {
                try {
                    processStatusMessage(message);
                    deleteMessage(message);
                } catch (Exception e) {
                    log.error("Failed to process status message: {}, Error: {}", 
                            message.messageId(), e.getMessage(), e);
                    handleFailedMessage(message, e);
                }
            }
        } catch (SqsException e) {
            log.error("Failed to poll messages from status queue: {}", e.getMessage(), e);
        }
    }

    private void processStatusMessage(Message message) throws Exception {
        String messageBody = message.body();
        ContentProcessEvent event = objectMapper.readValue(messageBody, ContentProcessEvent.class);
        
        // Add null safety checks
        String eventType = event.getEventType();
        String contentId = event.getContentId();
        String contentType = event.getContentType();
        
        log.info("Received content processing event: {}, ContentId: {}, ContentType: {}", 
                eventType, contentId, contentType);

        if (eventType == null) {
            log.error("Event type is null in message: {}", messageBody);
            throw new IllegalArgumentException("Event type cannot be null");
        }

        switch (eventType) {
            case "content.process.success":
                handleProcessingSuccess((ContentProcessSuccessEvent) event);
                break;
            case "content.process.failed":
                handleProcessingFailure((ContentProcessFailedEvent) event);
                break;
            default:
                log.warn("Unexpected event type received on response queue: {}", eventType);
        }
    }

    private void handleProcessingSuccess(ContentProcessSuccessEvent event) {
        log.info("Content processing completed successfully - ContentId: {}, ContentType: {}, ChunkCount: {}, S3Key: {}", 
                event.getContentId(), event.getContentType(), event.getChunkCount(), event.getS3Key());
        
        try {
            // Fetch documents from S3
            List<Document> documents = fetchDocumentsFromS3(event.getS3BucketName(), event.getS3Key());
            event.setDocuments(documents);
            
            contentProcessingStatusService.markProcessingComplete(
                    event.getContentId(),
                    event.getContentType(),
                    event.getChunkCount(),
                    documents,
                    event.getProcessingTimeMs()
            );
        } catch (Exception e) {
            log.error("Failed to fetch documents from S3 - ContentId: {}, S3Key: {}, Error: {}", 
                    event.getContentId(), event.getS3Key(), e.getMessage(), e);
            throw new RuntimeException("Failed to process success event due to S3 fetch error", e);
        }
    }

    private void handleProcessingFailure(ContentProcessFailedEvent event) {
        log.error("Content processing failed - ContentId: {}, ContentType: {}, Error: {}", 
                event.getContentId(), event.getContentType(), event.getErrorMessage());
        
        contentProcessingStatusService.markProcessingFailed(
                event.getContentId(),
                event.getContentType(),
                event.getErrorMessage(),
                event.getErrorCode(),
                event.getFailedStep()
        );
    }

    private void deleteMessage(Message message) {
        try {
            DeleteMessageRequest deleteRequest = DeleteMessageRequest.builder()
                    .queueUrl(knowledgeProcessingConfig.getProcessingQueueUrl())
                    .receiptHandle(message.receiptHandle())
                    .build();

            sqsClient.deleteMessage(deleteRequest);
            log.debug("Successfully deleted message: {}", message.messageId());
        } catch (SqsException e) {
            log.error("Failed to delete message: {}, Error: {}", message.messageId(), e.getMessage(), e);
        }
    }

    private List<Document> fetchDocumentsFromS3(String bucketName, String s3Key) {
        try {
            log.debug("Fetching documents from S3 - Bucket: {}, Key: {}", bucketName, s3Key);
            
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();
            
            try (ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(getObjectRequest)) {
                String jsonContent = new String(s3Object.readAllBytes());
                
                // Parse JSON to List<Document>
                TypeReference<List<Document>> typeRef = new TypeReference<List<Document>>() {};
                List<Document> documents = objectMapper.readValue(jsonContent, typeRef);
                
                log.info("Successfully fetched {} documents from S3 - Bucket: {}, Key: {}", 
                        documents.size(), bucketName, s3Key);
                
                return documents;
            }
        } catch (S3Exception e) {
            log.error("S3 error while fetching documents - Bucket: {}, Key: {}, Error: {}", 
                    bucketName, s3Key, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch documents from S3", e);
        } catch (Exception e) {
            log.error("Error parsing documents from S3 JSON - Bucket: {}, Key: {}, Error: {}", 
                    bucketName, s3Key, e.getMessage(), e);
            throw new RuntimeException("Failed to parse documents from S3 JSON", e);
        }
    }

    private void handleFailedMessage(Message message, Exception e) {
        try {
            String receiveCount = message.attributes().get("ApproximateReceiveCount");
            int count = receiveCount != null ? Integer.parseInt(receiveCount) : 0;
            
            if (count >= 3) {
                log.error("Message {} has exceeded max retry attempts ({}), it will be sent to DLQ", 
                        message.messageId(), count);
            } else {
                log.info("Message {} will be retried (attempt {})", message.messageId(), count + 1);
            }
        } catch (Exception ex) {
            log.error("Failed to handle failed message: {}", message.messageId(), ex);
        }
    }
}