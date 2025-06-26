package com.moksh.kontext.chat.repository;

import com.moksh.kontext.chat.entity.Chat;
import com.moksh.kontext.chat.entity.ChatMessage;
import org.springframework.ai.chat.messages.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    /**
     * Find all messages for a specific chat, ordered by creation date.
     * 
     * @param chatId The chat ID
     * @return List of chat messages
     */
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.chat.id = :chatId ORDER BY cm.createdAt ASC")
    List<ChatMessage> findByChatIdOrderByCreatedAtAsc(@Param("chatId") UUID chatId);

    /**
     * Find all messages for a specific chat with pagination.
     * 
     * @param chatId The chat ID
     * @param pageable Pagination information
     * @return Page of chat messages
     */
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.chat.id = :chatId ORDER BY cm.createdAt ASC")
    Page<ChatMessage> findByChatId(@Param("chatId") UUID chatId, Pageable pageable);

    /**
     * Find messages by chat ID and message type.
     * 
     * @param chatId The chat ID
     * @param type The message type
     * @return List of chat messages
     */
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.chat.id = :chatId AND cm.type = :type ORDER BY cm.createdAt ASC")
    List<ChatMessage> findByChatIdAndType(@Param("chatId") UUID chatId, @Param("type") MessageType type);

    /**
     * Count messages for a specific chat.
     * 
     * @param chatId The chat ID
     * @return Number of messages
     */
    @Query("SELECT COUNT(cm) FROM ChatMessage cm WHERE cm.chat.id = :chatId")
    long countByChatId(@Param("chatId") UUID chatId);

    /**
     * Find the last N messages for a chat.
     * 
     * @param chatId The chat ID
     * @param limit Number of messages to retrieve
     * @return List of recent chat messages
     */
    @Query(value = "SELECT cm FROM ChatMessage cm WHERE cm.chat.id = :chatId ORDER BY cm.createdAt DESC LIMIT :limit")
    List<ChatMessage> findLastMessagesByChatId(@Param("chatId") UUID chatId, @Param("limit") int limit);

    /**
     * Delete all messages for a specific chat.
     * 
     * @param chatId The chat ID
     */
    @Query("DELETE FROM ChatMessage cm WHERE cm.chat.id = :chatId")
    void deleteByChatId(@Param("chatId") UUID chatId);
}