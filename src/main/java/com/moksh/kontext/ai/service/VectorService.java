package com.moksh.kontext.ai.service;

import io.qdrant.client.QdrantClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class VectorService {

    private final VectorStore vectorStore;

    public void addDocuments(List<Document> documents) {
        log.info("Adding {} documents to vector store", documents.size());
        try {
            vectorStore.add(documents);
            log.info("Successfully added documents to vector store");
        } catch (Exception e) {
            log.error("Error adding documents to vector store", e);
            throw new RuntimeException("Failed to add documents to vector store", e);
        }
    }

    public void addDocument(String content, Map<String, Object> metadata) {
        Document document = new Document(content, metadata);
        addDocuments(List.of(document));
    }

    public void deleteDocuments(List<String> documentIds) {
        log.info("Deleting {} documents from vector store", documentIds.size());
        try {
            vectorStore.delete(documentIds);
            log.info("Successfully deleted documents from vector store");
        } catch (Exception e) {
            log.error("Error deleting documents from vector store", e);
            throw new RuntimeException("Failed to delete documents from vector store", e);
        }
    }
}