package com.moksh.kontext.knowledge_processing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moksh.kontext.knowledge_processing.config.KnowledgeProcessingConfig;
import com.moksh.kontext.knowledge_processing.constants.EventType;
import com.moksh.kontext.knowledge_processing.dto.event.ContentProcessEvent;
import com.moksh.kontext.knowledge_processing.dto.event.ContentProcessFailedEvent;
import com.moksh.kontext.knowledge_processing.dto.event.ContentProcessSuccessEvent;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
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

    private final S3Client s3Client;
    private final ObjectMapper objectMapper;
    private final ContentProcessingStatusService contentProcessingStatusService;

    @SqsListener(value = "${aws.sqs.processing-queue-name}", pollTimeoutSeconds = "20")
    public void receiveMessage(String messageBody) {
        try {
            processStatusMessage(messageBody);
        } catch (Exception e) {
            log.error("Failed to process status message: {}", e.getMessage(), e);
            throw new RuntimeException("Message processing failed", e);
        }
    }

    private void processStatusMessage(String messageBody) throws Exception {
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
            case EventType.CONTENT_PROCESS_SUCCESS:
                handleProcessingSuccess((ContentProcessSuccessEvent) event);
                break;
            case EventType.CONTENT_PROCESS_FAILED:
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
            
            // Delete the document from S3 after successful fetch
            deleteDocumentFromS3(event.getS3BucketName(), event.getS3Key());
            
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

    private void deleteDocumentFromS3(String bucketName, String s3Key) {
        try {
            log.debug("Deleting document from S3 - Bucket: {}, Key: {}", bucketName, s3Key);
            
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();
            
            s3Client.deleteObject(deleteObjectRequest);
            
            log.info("Successfully deleted document from S3 - Bucket: {}, Key: {}", bucketName, s3Key);
        } catch (S3Exception e) {
            log.error("S3 error while deleting document - Bucket: {}, Key: {}, Error: {}", 
                    bucketName, s3Key, e.getMessage(), e);
            throw new RuntimeException("Failed to delete document from S3", e);
        } catch (Exception e) {
            log.error("Error deleting document from S3 - Bucket: {}, Key: {}, Error: {}", 
                    bucketName, s3Key, e.getMessage(), e);
            throw new RuntimeException("Failed to delete document from S3", e);
        }
    }

}