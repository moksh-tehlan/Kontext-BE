package com.moksh.kontext.knowledge_processing.service;

import com.moksh.kontext.ai.service.VectorService;
import com.moksh.kontext.knowledge.entity.Knowledge;
import com.moksh.kontext.knowledge.service.KnowledgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContentProcessingStatusService {

    private final VectorService vectorService;
    private final KnowledgeService knowledgeService;

    public void markProcessingComplete(String contentId, String contentType, Integer chunkCount,
                                       List<Document> documents, Long processingTimeMs) {
        log.info("Marking {} content {} as processing complete. Chunks: {}, Processing time: {}ms",
                contentType, contentId, chunkCount, processingTimeMs);

        vectorService.addDocuments(documents);
        knowledgeService.markProcessingSuccess(UUID.fromString(contentId));

        log.info("Content processing completed for {}: {} documents processed", contentId, documents.size());
    }

    public void markProcessingFailed(String contentId, String contentType, String errorMessage,
                                     String errorCode, String failedStep) {
        log.error("Marking {} content {} as processing failed. Error: {} ({}), Failed at: {}",
                contentType, contentId, errorMessage, errorCode, failedStep);

        knowledgeService.markProcessingFailed(UUID.fromString(contentId),errorMessage);
    }
}