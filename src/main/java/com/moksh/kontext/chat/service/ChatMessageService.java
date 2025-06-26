package com.moksh.kontext.chat.service;

import com.moksh.kontext.chat.entity.Chat;
import com.moksh.kontext.chat.entity.ChatMessage;
import com.moksh.kontext.chat.repository.ChatMessageRepository;
import com.moksh.kontext.chat.service.ChatService;
import com.moksh.kontext.common.exception.ResourceNotFoundException;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@Primary
public class ChatMessageService implements ChatMemoryRepository{

    private final ChatMessageRepository chatMessageRepository;
    private final ChatService chatService;

    @Autowired
    public ChatMessageService(
            ChatMessageRepository chatMessageRepository,
            ChatService chatService) {

        this.chatMessageRepository = chatMessageRepository;
        this.chatService = chatService;
    }

    /**
     * Create a new chat message.
     *
     * @param chatId  The chat ID
     * @param content The message content
     * @param type    The message type
     * @return The created chat message
     */
    @Transactional
    public ChatMessage createMessage(UUID chatId, String content, MessageType type) {
        log.debug("Creating message for chat: {}", chatId);

        Chat chat = getChatEntity(chatId);

        ChatMessage message = new ChatMessage();
        message.setChat(chat);
        message.setContent(content);
        message.setType(type);

        ChatMessage savedMessage = chatMessageRepository.save(message);
        log.debug("Created message with ID: {} for chat: {}", savedMessage.getId(), chatId);

        return savedMessage;
    }

    /**
     * Get all messages for a chat, ordered by creation date.
     *
     * @param chatId The chat ID
     * @return List of chat messages
     */
    public List<ChatMessage> getChatMessages(UUID chatId) {
        log.debug("Retrieving messages for chat: {}", chatId);

        // Verify chat exists
        getChatEntity(chatId);

        return chatMessageRepository.findByChatIdOrderByCreatedAtAsc(chatId);
    }

    /**
     * Get chat messages with pagination.
     *
     * @param chatId   The chat ID
     * @param pageable Pagination information
     * @return Page of chat messages
     */
    public Page<ChatMessage> getChatMessages(UUID chatId, Pageable pageable) {
        log.debug("Retrieving paginated messages for chat: {}", chatId);

        // Verify chat exists
        getChatEntity(chatId);

        return chatMessageRepository.findByChatId(chatId, pageable);
    }

    /**
     * Get messages by type for a specific chat.
     *
     * @param chatId The chat ID
     * @param type   The message type
     * @return List of chat messages
     */
    public List<ChatMessage> getMessagesByType(UUID chatId, MessageType type) {
        log.debug("Retrieving messages of type {} for chat: {}", type, chatId);

        // Verify chat exists
        getChatEntity(chatId);

        return chatMessageRepository.findByChatIdAndType(chatId, type);
    }

    /**
     * Get the last N messages for a chat.
     *
     * @param chatId The chat ID
     * @param limit  Number of messages to retrieve
     * @return List of recent chat messages
     */
    public List<ChatMessage> getLastMessages(UUID chatId, int limit) {
        log.debug("Retrieving last {} messages for chat: {}", limit, chatId);

        // Verify chat exists
        getChatEntity(chatId);

        return chatMessageRepository.findLastMessagesByChatId(chatId, limit);
    }

    /**
     * Get message count for a chat.
     *
     * @param chatId The chat ID
     * @return Number of messages
     */
    public long getMessageCount(UUID chatId) {
        log.debug("Counting messages for chat: {}", chatId);

        // Verify chat exists
        getChatEntity(chatId);

        return chatMessageRepository.countByChatId(chatId);
    }

    /**
     * Get a specific message by ID.
     *
     * @param messageId The message ID
     * @return The chat message
     * @throws ResourceNotFoundException if message not found
     */
    public ChatMessage getMessageById(UUID messageId) {
        log.debug("Retrieving message: {}", messageId);

        return chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found with id: " + messageId));
    }

    /**
     * Update message content.
     *
     * @param messageId The message ID
     * @param content   The new content
     * @return The updated message
     * @throws ResourceNotFoundException if message not found
     */
    @Transactional
    public ChatMessage updateMessage(UUID messageId, String content) {
        log.debug("Updating message: {}", messageId);

        ChatMessage message = getMessageById(messageId);
        message.setContent(content);

        ChatMessage updatedMessage = chatMessageRepository.save(message);
        log.debug("Updated message: {}", messageId);

        return updatedMessage;
    }

    /**
     * Delete a specific message.
     *
     * @param messageId The message ID
     * @throws ResourceNotFoundException if message not found
     */
    @Transactional
    public void deleteMessage(UUID messageId) {
        log.debug("Deleting message: {}", messageId);

        if (!chatMessageRepository.existsById(messageId)) {
            throw new ResourceNotFoundException("Message not found with id: " + messageId);
        }

        chatMessageRepository.deleteById(messageId);
        log.debug("Deleted message: {}", messageId);
    }

    /**
     * Delete all messages for a chat.
     *
     * @param chatId The chat ID
     */
    @Transactional
    public void deleteAllChatMessages(UUID chatId) {
        log.debug("Deleting all messages for chat: {}", chatId);

        // Verify chat exists
        getChatEntity(chatId);

        chatMessageRepository.deleteByChatId(chatId);
        log.debug("Deleted all messages for chat: {}", chatId);
    }

    /**
     * Add user message to chat.
     *
     * @param chatId  The chat ID
     * @param content The message content
     * @return The created user message
     */
    @Transactional
    public ChatMessage addUserMessage(UUID chatId, String content) {
        return createMessage(chatId, content, MessageType.USER);
    }

    /**
     * Add assistant message to chat.
     *
     * @param chatId  The chat ID
     * @param content The message content
     * @return The created assistant message
     */
    @Transactional
    public ChatMessage addAssistantMessage(UUID chatId, String content) {
        return createMessage(chatId, content, MessageType.ASSISTANT);
    }

    /**
     * Add system message to chat.
     *
     * @param chatId  The chat ID
     * @param content The message content
     * @return The created system message
     */
    @Transactional
    public ChatMessage addSystemMessage(UUID chatId, String content) {
        return createMessage(chatId, content, MessageType.SYSTEM);
    }

    /**
     * Helper method to get chat entity and verify it exists.
     *
     * @param chatId The chat ID
     * @return The chat entity
     * @throws ResourceNotFoundException if chat not found
     */
    private Chat getChatEntity(UUID chatId) {
        return chatService.getChatEntityById(chatId);
    }

    @Override
    public List<String> findConversationIds() {
        List<ChatMessage> messages = chatMessageRepository.findAll();
        if (!messages.isEmpty()) {
            return messages.stream()
                    .map(chatMessage -> chatMessage.getChat().getId().toString())
                    .distinct()
                    .toList();
        }
        return List.of();
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        UUID chatId = UUID.fromString(conversationId);
        List<ChatMessage> messages = chatMessageRepository.findByChatIdOrderByCreatedAtAsc(chatId);
        if (messages != null && !messages.isEmpty()) {
            return messages.stream()
                    .map(this::mapMessage)
                    .toList();
        }
        return List.of();
    }

    public Message mapMessage(ChatMessage chatMessage) {

        return switch (chatMessage.getType()) {
            case USER -> new UserMessage(chatMessage.getContent());
            case ASSISTANT -> new AssistantMessage(chatMessage.getContent());
            case SYSTEM -> new SystemMessage(chatMessage.getContent());
            // The content is always stored empty for ToolResponseMessages.
            // If we want to capture the actual content, we need to extend
            // AddBatchPreparedStatement to support it.
            case TOOL -> new ToolResponseMessage(List.of());
        };
    }

    @Override
    @Transactional
    public void saveAll(String conversationId, List<Message> messages) {
        UUID chatId = UUID.fromString(conversationId);
        Chat chat = getChatEntity(chatId);

        for (Message message : messages) {
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setChat(chat);
            chatMessage.setContent(message.getText());
            chatMessage.setType(message.getMessageType());
            chatMessageRepository.save(chatMessage);
        }
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        UUID chatId = UUID.fromString(conversationId);
        getChatEntity(chatId);
        chatMessageRepository.deleteByChatId(chatId);
    }
}