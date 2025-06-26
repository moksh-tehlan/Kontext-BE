package com.moksh.kontext.chat.service;

import com.moksh.kontext.chat.dto.ChatDto;
import com.moksh.kontext.chat.dto.CreateChatDto;
import com.moksh.kontext.chat.dto.UpdateChatDto;
import com.moksh.kontext.chat.entity.Chat;
import com.moksh.kontext.chat.mapper.ChatMapper;
import com.moksh.kontext.chat.repository.ChatRepository;
import com.moksh.kontext.common.exception.ResourceNotFoundException;
import com.moksh.kontext.common.util.SecurityContextUtil;
import com.moksh.kontext.project.entity.Project;
import com.moksh.kontext.project.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class ChatService {

    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ChatMapper chatMapper;

    public ChatDto createChat(CreateChatDto createChatDto) {
        UUID currentUserId = SecurityContextUtil.getCurrentUserId();
        Project project = projectRepository.findByIdAndUserIdAndIsActiveTrue(createChatDto.getProjectId(), currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        Chat chat = chatMapper.toEntity(createChatDto);
        chat.setProject(project);
        
        Chat savedChat = chatRepository.save(chat);
        return chatMapper.toDto(savedChat);
    }

    @Transactional(readOnly = true)
    public List<ChatDto> getProjectChats(UUID projectId) {
        UUID currentUserId = SecurityContextUtil.getCurrentUserId();
        projectRepository.findByIdAndUserIdAndIsActiveTrue(projectId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        List<Chat> chats = chatRepository.findByProjectIdAndIsActiveTrue(projectId);
        
        return chats.stream()
                .map(chatMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<ChatDto> getProjectChats(UUID projectId, Pageable pageable) {
        UUID currentUserId = SecurityContextUtil.getCurrentUserId();
        projectRepository.findByIdAndUserIdAndIsActiveTrue(projectId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        Page<Chat> chats = chatRepository.findByProjectIdAndIsActiveTrue(projectId, pageable);
        
        return chats.map(chatMapper::toDto);
    }

    @Transactional(readOnly = true)
    public ChatDto getChatById(UUID chatId) {
        UUID currentUserId = SecurityContextUtil.getCurrentUserId();
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat not found"));

        projectRepository.findByIdAndUserIdAndIsActiveTrue(chat.getProject().getId(), currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        return chatMapper.toDto(chat);
    }

    @Transactional(readOnly = true)
    public Chat getChatEntityById(UUID chatId) {
        UUID currentUserId = SecurityContextUtil.getCurrentUserId();
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat not found"));

        projectRepository.findByIdAndUserIdAndIsActiveTrue(chat.getProject().getId(), currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        return chat;
    }

    public ChatDto updateChat(UUID chatId, UpdateChatDto updateChatDto) {
        UUID currentUserId = SecurityContextUtil.getCurrentUserId();
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat not found"));

        projectRepository.findByIdAndUserIdAndIsActiveTrue(chat.getProject().getId(), currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        chatMapper.updateEntityFromDto(updateChatDto, chat);
        Chat updatedChat = chatRepository.save(chat);
        
        return chatMapper.toDto(updatedChat);
    }

    public void deleteChat(UUID chatId) {
        UUID currentUserId = SecurityContextUtil.getCurrentUserId();
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat not found"));

        projectRepository.findByIdAndUserIdAndIsActiveTrue(chat.getProject().getId(), currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        chat.setIsActive(false);
        chatRepository.save(chat);
    }

    @Transactional(readOnly = true)
    public Page<ChatDto> searchChats(UUID projectId, String searchTerm, Pageable pageable) {
        UUID currentUserId = SecurityContextUtil.getCurrentUserId();
        projectRepository.findByIdAndUserIdAndIsActiveTrue(projectId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        Page<Chat> chats = chatRepository.findByProjectIdAndSearchTerm(projectId, searchTerm, pageable);
        
        return chats.map(chatMapper::toDto);
    }
}