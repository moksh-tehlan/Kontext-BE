package com.moksh.kontext.ai.service;

import com.moksh.kontext.knowledge.service.KnowledgeService;
import com.moksh.kontext.project.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagChatService {
    private final VectorStore vectorStore;
    private final ChatModel chatModel;
    private final KnowledgeService knowledgeService;

    public String chatWithContext(String message, UUID projectId) {
        log.info("Processing RAG chat request for project: {}", projectId);
        
        try {
            List<String> knowledgeIds = knowledgeService.getProjectKnowledge(projectId).stream().map(project -> project.getId().toString()).toList();

            // Create filter for project context
            Filter.Expression knowledgeFilter = new FilterExpressionBuilder()
                    .in("knowledge_id", knowledgeIds)
                    .build();

            DocumentRetriever retriever = VectorStoreDocumentRetriever.builder()
                    .vectorStore(vectorStore)
                    .similarityThreshold(0.73)
                    .topK(5)
                    .filterExpression(knowledgeFilter)
                    .build();

            ChatClient ragChatClient = ChatClient.builder(chatModel)
                    .defaultAdvisors(a -> a.param(QuestionAnswerAdvisor.RETRIEVED_DOCUMENTS, retriever))
                    .build();

            String response = ragChatClient.prompt()
                    .user(message)
                    .call()
                    .content();

            log.info("Generated RAG response for project: {}", projectId);
            return response;
        } catch (Exception e) {
            log.error("Error generating RAG response for project: {}", projectId, e);
            throw new RuntimeException("Failed to generate RAG response", e);
        }
    }
}