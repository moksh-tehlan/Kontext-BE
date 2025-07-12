package com.moksh.kontext.ai.advisor;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.util.Assert;
import reactor.core.scheduler.Scheduler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class KontextChatAdvisor implements BaseAdvisor {

    public static final String RETRIEVED_DOCUMENTS = "kontext_retrieved_documents";
    private static final PromptTemplate DEFAULT_PROMPT_TEMPLATE = new PromptTemplate("""
            {agent_instruction}
            
            User: {user_display_name}
            Query: {query}
            
            Context information is below, surrounded by ---------------------
            
            ---------------------
            {question_answer_context}
            ---------------------
            
            Given the context and provided history information and not prior knowledge,
            reply to the user comment. If the answer is not in the context, inform
            the user that you can't answer the question. Address the user by their name when appropriate.
            """);

    private final List<Document> documents;
    private final String agentInstruction;
    private final String userDisplayName;
    private final PromptTemplate promptTemplate;
    private final Scheduler scheduler;

    public KontextChatAdvisor(List<Document> documents, String agentInstruction, String userDisplayName) {
        this.documents = documents;
        this.agentInstruction = agentInstruction;
        this.userDisplayName = userDisplayName;
        this.promptTemplate = DEFAULT_PROMPT_TEMPLATE;
        this.scheduler = BaseAdvisor.DEFAULT_SCHEDULER;
    }

    public static Builder builder(List<Document> documents) {
        return new Builder(documents);
    }

    public static final class Builder {
        List<Document> documents;
        String agentInstruction;
        String userDisplayName;

        private Builder(List<Document> documents) {
            Assert.notNull(documents, "The documents must not be null!");
            this.documents = documents;
        }

        public Builder withAgentInstruction(String agentInstruction) {
            this.agentInstruction = agentInstruction;
            return this;
        }

        public Builder withUserDisplayName(String userDisplayName) {
            this.userDisplayName = userDisplayName;
            return this;
        }

        public KontextChatAdvisor build() {
            return new KontextChatAdvisor(
                    this.documents,
                    this.agentInstruction,
                    this.userDisplayName
            );
        }
    }

    @Override
    public Scheduler getScheduler() {
        return this.scheduler;
    }

    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        Map<String, Object> context = new HashMap<>(chatClientRequest.context());
        context.put(RETRIEVED_DOCUMENTS, documents);
        String documentContext = documents == null ? ""
                : documents.stream().map(Document::getText).collect(Collectors.joining(System.lineSeparator()));

        UserMessage userMessage = chatClientRequest.prompt().getUserMessage();
        String agentInstructionText = agentInstruction != null && !agentInstruction.trim().isEmpty() 
                ? agentInstruction 
                : "";
        String userDisplayNameText = userDisplayName != null && !userDisplayName.trim().isEmpty()
                ? userDisplayName
                : "User";
        String augmentedUserText = this.promptTemplate
                .render(Map.of(
                        "agent_instruction", agentInstructionText,
                        "user_display_name", userDisplayNameText,
                        "query", userMessage.getText(), 
                        "question_answer_context", documentContext
                ));

        // 4. Update ChatClientRequest with augmented prompt.
        return chatClientRequest.mutate()
                .prompt(chatClientRequest.prompt().augmentUserMessage(augmentedUserText))
                .context(context)
                .build();
    }

    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        ChatResponse.Builder chatResponseBuilder;
        if (chatClientResponse.chatResponse() == null) {
            chatResponseBuilder = ChatResponse.builder();
        } else {
            chatResponseBuilder = ChatResponse.builder().from(chatClientResponse.chatResponse());
        }
        chatResponseBuilder.metadata(RETRIEVED_DOCUMENTS, chatClientResponse.context().get(RETRIEVED_DOCUMENTS));
        return ChatClientResponse.builder()
                .chatResponse(chatResponseBuilder.build())
                .context(chatClientResponse.context())
                .build();
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
