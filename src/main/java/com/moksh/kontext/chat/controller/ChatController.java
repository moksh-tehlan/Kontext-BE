package com.moksh.kontext.chat.controller;

import com.moksh.kontext.chat.dto.ChatRequest;
import com.moksh.kontext.ai.service.RagChatService;
import com.moksh.kontext.chat.dto.ChatDto;
import com.moksh.kontext.chat.dto.ChatMessageDto;
import com.moksh.kontext.chat.dto.CreateChatDto;
import com.moksh.kontext.chat.dto.UpdateChatDto;
import com.moksh.kontext.chat.entity.ChatMessage;
import com.moksh.kontext.chat.mapper.ChatMapper;
import com.moksh.kontext.chat.service.ChatService;
import com.moksh.kontext.chat.service.ChatMessageService;
import com.moksh.kontext.common.response.ApiResponse;
import com.moksh.kontext.common.response.PageResponse;
import com.moksh.kontext.common.util.SecurityContextUtil;
import com.moksh.kontext.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/chats")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;
    private final ChatMessageService chatMessageService;
    private final ChatMapper chatMapper;
    private final RagChatService ragChatService;

    @PostMapping
    public ApiResponse<ChatDto> createChat(@Valid @RequestBody CreateChatDto createChatDto) {
        log.debug("POST /chats - Creating new chat");
        
        ChatDto createdChat = chatService.createChat(createChatDto);
        return ApiResponse.success(createdChat, "Chat created successfully", 201);
    }

    @GetMapping("/project/{projectId}")
    public ApiResponse<List<ChatDto>> getProjectChats(@PathVariable UUID projectId) {
        log.debug("GET /chats/project/{} - Fetching chats for project", projectId);
        
        List<ChatDto> chats = chatService.getProjectChats(projectId);
        return ApiResponse.success(chats, "Chats retrieved successfully");
    }

    @GetMapping("/project/{projectId}/paginated")
    public ApiResponse<PageResponse<ChatDto>> getProjectChatsPaginated(
            @PathVariable UUID projectId,
            @PageableDefault(size = 10) Pageable pageable) {
        log.debug("GET /chats/project/{}/paginated - Fetching chats for project paginated", projectId);
        
        Page<ChatDto> chats = chatService.getProjectChats(projectId, pageable);
        PageResponse<ChatDto> pageResponse = PageResponse.of(chats);
        
        return ApiResponse.success(pageResponse, "Chats retrieved successfully");
    }

    @GetMapping("/{id}")
    public ApiResponse<ChatDto> getChatById(@PathVariable UUID id) {
        log.debug("GET /chats/{} - Fetching chat by id", id);
        
        ChatDto chat = chatService.getChatById(id);
        return ApiResponse.success(chat, "Chat retrieved successfully");
    }

    @PutMapping("/{id}")
    public ApiResponse<ChatDto> updateChat(@PathVariable UUID id, @Valid @RequestBody UpdateChatDto updateChatDto) {
        log.debug("PUT /chats/{} - Updating chat", id);
        
        ChatDto updatedChat = chatService.updateChat(id, updateChatDto);
        return ApiResponse.success(updatedChat, "Chat updated successfully");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteChat(@PathVariable UUID id) {
        log.debug("DELETE /chats/{} - Deleting chat", id);
        
        chatService.deleteChat(id);
        return ApiResponse.success(null, "Chat deleted successfully", 204);
    }

    @GetMapping("/project/{projectId}/search")
    public ApiResponse<PageResponse<ChatDto>> searchChats(
            @PathVariable UUID projectId,
            @RequestParam String searchTerm,
            @PageableDefault(size = 10) Pageable pageable) {
        log.debug("GET /chats/project/{}/search - Searching chats with term: {}", projectId, searchTerm);
        
        Page<ChatDto> chats = chatService.searchChats(projectId, searchTerm, pageable);
        PageResponse<ChatDto> pageResponse = PageResponse.of(chats);
        
        return ApiResponse.success(pageResponse, "Chats searched successfully");
    }

    @GetMapping("/{id}/history")
    public ApiResponse<List<ChatMessageDto>> getChatHistory(@PathVariable UUID id) {
        log.debug("GET /chats/{}/history - Fetching chat history", id);
        
        List<ChatMessage> messages = chatMessageService.getChatMessages(id);
        List<ChatMessageDto> messageDtos = messages.stream()
                .map(chatMapper::toDto)
                .toList();
        return ApiResponse.success(messageDtos, "Chat history retrieved successfully");
    }

    @GetMapping("/{id}/history/paginated")
    public ApiResponse<PageResponse<ChatMessageDto>> getChatHistoryPaginated(
            @PathVariable UUID id,
            @PageableDefault(size = 20) Pageable pageable) {
        log.debug("GET /chats/{}/history/paginated - Fetching chat history paginated", id);
        
        Page<ChatMessage> messages = chatMessageService.getChatMessages(id, pageable);
        Page<ChatMessageDto> messageDtos = messages.map(chatMapper::toDto);
        PageResponse<ChatMessageDto> pageResponse = PageResponse.of(messageDtos);
        
        return ApiResponse.success(pageResponse, "Chat history retrieved successfully");
    }

    @PostMapping("{id}/chat")
    public ApiResponse<String> chatWithAI(@Valid @RequestBody ChatRequest chatRequest, @PathVariable UUID id) {
        UUID projectId = chatService.getChatById(id).getProjectId();
        
        // Get current user and their display name
        User currentUser = SecurityContextUtil.getCurrentUserOrThrow();
        String userDisplayName = currentUser.getDisplayName();

        String aiResponse = ragChatService.chatWithContext(chatRequest.getQuery(), projectId, id, userDisplayName);

        return ApiResponse.success(aiResponse, "AI chat response generated successfully");
    }
}