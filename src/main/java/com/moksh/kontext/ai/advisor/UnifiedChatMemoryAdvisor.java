package com.moksh.kontext.ai.advisor;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import com.moksh.kontext.chat.service.ChatMessageService;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.Ordered;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
public class UnifiedChatMemoryAdvisor implements BaseAdvisor {

    private final VectorStore chatVectorStore;
    private final ChatMessageService chatMessageService;
    private final String conversationId;
    private final int maxMessages;
    private final int vectorTopK;
    private final double vectorSimilarityThreshold;
    private final Scheduler scheduler;
    private String currentUserMessage;

    @Builder
    public UnifiedChatMemoryAdvisor(
            VectorStore chatVectorStore,
            ChatMessageService chatMessageService,
            String conversationId,
            Integer maxMessages,
            Integer vectorTopK,
            Double vectorSimilarityThreshold) {
        
        this.chatVectorStore = chatVectorStore;
        this.chatMessageService = chatMessageService;
        this.conversationId = conversationId;
        this.maxMessages = maxMessages != null ? maxMessages : 10;
        this.vectorTopK = vectorTopK != null ? vectorTopK : 5;
        this.vectorSimilarityThreshold = vectorSimilarityThreshold != null ? vectorSimilarityThreshold : 0.7;
        this.scheduler = Schedulers.boundedElastic();
    }

    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain chain) {
        try {
            String userQuery = extractUserMessage(request);
            if (userQuery == null || userQuery.trim().isEmpty()) {
                return request;
            }

            // Store user message for later saving
            currentUserMessage = userQuery;

            log.debug("Processing unified chat memory for conversation: {}", conversationId);

            // 1. Get recent structured history from PostgreSQL
            List<Message> recentMessages = getRecentMessages();
            
            // 2. Get semantic context from Qdrant vector store
            List<Document> semanticContext = getSemanticContext(userQuery);
            
            // 3. Combine both contexts intelligently
            List<Message> unifiedMemory = combineMemoryContext(recentMessages, semanticContext, userQuery);
            
            // 4. Add unified memory to request context
            if (!unifiedMemory.isEmpty()) {
                log.debug("Adding {} unified memory messages to conversation: {}", 
                         unifiedMemory.size(), conversationId);
                
                // Add memory messages to the prompt
                return request.mutate()
                    .prompt(request.prompt().augmentSystemMessage(formatMemoryContext(unifiedMemory)))
                    .build();
            }
            
        } catch (Exception e) {
            log.error("Error in UnifiedChatMemoryAdvisor before(): {}", e.getMessage(), e);
        }
        
        return request;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse response, AdvisorChain chain) {
        try {
            log.debug("Saving response to unified chat memory for conversation: {}", conversationId);
            
            // Extract messages from request and response
            List<Message> messagesToSave = extractMessagesForSaving(response);
            
            if (!messagesToSave.isEmpty()) {
                // Save to PostgreSQL (structured storage)
                saveToPostgreSQL(messagesToSave);
                
                // Save to Qdrant (vector storage for semantic search)
                saveToVectorStore(messagesToSave);
                
                log.debug("Successfully saved {} messages to unified storage for conversation: {}", 
                         messagesToSave.size(), conversationId);
            }
            
        } catch (Exception e) {
            log.error("Error in UnifiedChatMemoryAdvisor after(): {}", e.getMessage(), e);
        }
        
        return response;
    }

    private String formatMemoryContext(List<Message> messages) {
        if (messages.isEmpty()) {
            return "";
        }
        
        StringBuilder context = new StringBuilder("\n--- Chat History ---\n");
        for (Message message : messages) {
            String role = message.getMessageType().toString().toLowerCase();
            context.append(role).append(": ").append(message.getText()).append("\n");
        }
        context.append("--- End Chat History ---\n");
        
        return context.toString();
    }

    private String extractUserMessage(ChatClientRequest request) {
        return request.prompt().getUserMessage() != null ? 
               request.prompt().getUserMessage().getText() : null;
    }

    private List<Message> getRecentMessages() {
        try {
            UUID chatId = UUID.fromString(conversationId);
            return chatMessageService.getLastMessages(chatId, maxMessages)
                .stream()
                .map(chatMessageService::mapMessage)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Failed to retrieve recent messages for conversation {}: {}", 
                    conversationId, e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<Document> getSemanticContext(String userQuery) {
        try {
            if (chatVectorStore != null) {
                SearchRequest searchRequest = SearchRequest.builder()
                    .query(userQuery)
                    .topK(vectorTopK)
                    .similarityThreshold(vectorSimilarityThreshold)
                    .build();
                    
                return chatVectorStore.similaritySearch(searchRequest);
            }
        } catch (Exception e) {
            log.warn("Failed to retrieve semantic context for conversation {}: {}", 
                    conversationId, e.getMessage());
        }
        return new ArrayList<>();
    }

    private List<Message> combineMemoryContext(List<Message> recentMessages, 
                                             List<Document> semanticContext, 
                                             String userQuery) {
        List<Message> unifiedMemory = new ArrayList<>();
        
        // Add recent structured messages (priority to recent conversation flow)
        unifiedMemory.addAll(recentMessages);
        
        // Add relevant semantic context as system messages if available and relevant
        if (!semanticContext.isEmpty()) {
            for (Document doc : semanticContext) {
                String content = doc.getText();
                // Only add if it's not already in recent messages (avoid duplication)
                boolean isDuplicate = recentMessages.stream()
                    .anyMatch(msg -> msg.getText().contains(content.substring(0, 
                            Math.min(content.length(), 100))));
                
                if (!isDuplicate && isRelevantToQuery(content, userQuery)) {
                    // Add semantic context as system context
                    unifiedMemory.add(new org.springframework.ai.chat.messages.SystemMessage(
                        "Previous relevant context: " + content));
                }
            }
        }
        
        return unifiedMemory;
    }

    private boolean isRelevantToQuery(String content, String query) {
        // Simple relevance check - can be enhanced with more sophisticated matching
        String[] queryWords = query.toLowerCase().split("\\s+");
        String lowerContent = content.toLowerCase();
        
        int matches = 0;
        for (String word : queryWords) {
            if (word.length() > 3 && lowerContent.contains(word)) {
                matches++;
            }
        }
        
        // Consider relevant if at least 30% of query words appear in content
        return matches >= Math.max(1, queryWords.length * 0.3);
    }

    private List<Message> extractMessagesForSaving(ChatClientResponse response) {
        List<Message> messagesToSave = new ArrayList<>();
        
        // Add the user message if stored
        if (currentUserMessage != null && !currentUserMessage.trim().isEmpty()) {
            messagesToSave.add(new UserMessage(currentUserMessage));
        }
        
        // Add the assistant response
        if (response.chatResponse() != null && response.chatResponse().getResult() != null && 
            response.chatResponse().getResult().getOutput() != null) {
            String assistantContent = response.chatResponse().getResult().getOutput().getText();
            if (assistantContent != null && !assistantContent.trim().isEmpty()) {
                messagesToSave.add(new AssistantMessage(assistantContent));
            }
        }
        
        return messagesToSave;
    }

    private void saveToPostgreSQL(List<Message> messages) {
        try {
            UUID chatId = UUID.fromString(conversationId);
            for (Message message : messages) {
                if (message instanceof UserMessage) {
                    chatMessageService.addUserMessage(chatId, message.getText());
                } else if (message instanceof AssistantMessage) {
                    chatMessageService.addAssistantMessage(chatId, message.getText());
                }
            }
            log.debug("Saved {} messages to PostgreSQL for conversation: {}", 
                     messages.size(), conversationId);
        } catch (Exception e) {
            log.error("Failed to save messages to PostgreSQL for conversation {}: {}", 
                     conversationId, e.getMessage());
        }
    }

    private void saveToVectorStore(List<Message> messages) {
        try {
            if (chatVectorStore != null) {
                List<Document> documentsToAdd = messages.stream()
                    .filter(msg -> msg.getText() != null && !msg.getText().trim().isEmpty())
                    .map(msg -> Document.builder()
                        .text(msg.getText())
                        .metadata("conversation_id", conversationId)
                        .metadata("message_type", msg.getMessageType().toString())
                        .metadata("timestamp", String.valueOf(System.currentTimeMillis()))
                        .build())
                    .collect(Collectors.toList());
                
                if (!documentsToAdd.isEmpty()) {
                    chatVectorStore.accept(documentsToAdd);
                    log.debug("Saved {} messages to vector store for conversation: {}", 
                             documentsToAdd.size(), conversationId);
                }
            }
        } catch (Exception e) {
            log.error("Failed to save messages to vector store for conversation {}: {}", 
                     conversationId, e.getMessage());
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 100; // Execute after other advisors but before response
    }

    @Override
    public Scheduler getScheduler() {
        return scheduler;
    }

    public static class UnifiedChatMemoryAdvisorBuilder {
        public UnifiedChatMemoryAdvisorBuilder conversationId(String conversationId) {
            this.conversationId = conversationId;
            return this;
        }
        
        public UnifiedChatMemoryAdvisorBuilder maxMessages(int maxMessages) {
            this.maxMessages = maxMessages;
            return this;
        }
        
        public UnifiedChatMemoryAdvisorBuilder vectorTopK(int vectorTopK) {
            this.vectorTopK = vectorTopK;
            return this;
        }
        
        public UnifiedChatMemoryAdvisorBuilder vectorSimilarityThreshold(double threshold) {
            this.vectorSimilarityThreshold = threshold;
            return this;
        }
    }
}