package com.moksh.kontext.ai.service;

import com.moksh.kontext.ai.advisor.KontextChatAdvisor;
import com.moksh.kontext.ai.advisor.UnifiedChatMemoryAdvisor;
import com.moksh.kontext.chat.repository.ChatMessageRepository;
import com.moksh.kontext.chat.service.ChatMessageService;
import com.moksh.kontext.chat.service.ChatService;
import com.moksh.kontext.knowledge.service.KnowledgeService;
import com.moksh.kontext.project.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class RagChatService {
    private final VectorStore vectorStore;
    private final VectorStore chatVectorStore;
    private final ChatModel chatModel;
    private final KnowledgeService knowledgeService;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatService chatService;

    @Autowired
    public RagChatService(
            VectorStore vectorStore,
            @Qualifier("chat_vector_store") VectorStore chatVectorStore,
            ChatModel chatModel,
            KnowledgeService knowledgeService,
            ChatMessageRepository chatMessageRepository,
            ChatService chatService
    ) {
        this.vectorStore = vectorStore;
        this.chatVectorStore = chatVectorStore;
        this.chatModel = chatModel;
        this.knowledgeService = knowledgeService;
        this.chatMessageRepository = chatMessageRepository;
        this.chatService = chatService;
    }

    public String chatWithContext(String message, UUID projectId, UUID chatId) {
        log.info("Processing RAG chat request for project: {}", projectId);

        try {
            String[] knowledgeIds = knowledgeService.getProjectKnowledge(projectId).stream()
                    .map(project -> project.getId().toString()).toArray(String[]::new);

            Filter.Expression knowledgeFilter = new FilterExpressionBuilder()
                    .in("knowledge_id", knowledgeIds)
                    .build();

            DocumentRetriever retriever = VectorStoreDocumentRetriever.builder()
                    .vectorStore(vectorStore)
                    .similarityThreshold(0.6)
                    .topK(5)
                    .filterExpression(knowledgeFilter)
                    .build();

            List<Document> documents = retriever.retrieve(new Query(message));

            ChatMessageService chatMessageService = new ChatMessageService(chatMessageRepository,chatService);

            ChatClient ragChatClient = ChatClient.builder(chatModel)
                    .defaultAdvisors(KontextChatAdvisor.builder(documents).build())
                    .defaultAdvisors(UnifiedChatMemoryAdvisor.builder()
                            .chatVectorStore(chatVectorStore)
                            .chatMessageService(chatMessageService)
                            .conversationId(chatId.toString())
                            .maxMessages(10)
                            .vectorTopK(5)
                            .vectorSimilarityThreshold(0.7)
                            .build())
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