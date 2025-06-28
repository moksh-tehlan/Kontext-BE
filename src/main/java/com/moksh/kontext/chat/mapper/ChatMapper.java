package com.moksh.kontext.chat.mapper;

import com.moksh.kontext.chat.dto.ChatDto;
import com.moksh.kontext.chat.dto.ChatMessageDto;
import com.moksh.kontext.chat.dto.CreateChatDto;
import com.moksh.kontext.chat.dto.UpdateChatDto;
import com.moksh.kontext.chat.entity.Chat;
import com.moksh.kontext.chat.entity.ChatMessage;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ChatMapper {

    @Autowired
    private ModelMapper modelMapper;

    public ChatDto toDto(Chat chat) {
        ChatDto chatDto = modelMapper.map(chat, ChatDto.class);
        if (chat.getProject() != null) {
            chatDto.setProjectId(chat.getProject().getId());
        }
        return chatDto;
    }

    public Chat toEntity(CreateChatDto createChatDto) {
        return modelMapper.map(createChatDto, Chat.class);
    }

    public void updateEntityFromDto(UpdateChatDto updateChatDto, Chat chat) {
        if (updateChatDto.getName() != null) {
            chat.setName(updateChatDto.getName());
        }
    }

    public ChatMessageDto toDto(ChatMessage chatMessage) {
        ChatMessageDto chatMessageDto = modelMapper.map(chatMessage, ChatMessageDto.class);
        if (chatMessage.getChat() != null) {
            chatMessageDto.setChatId(chatMessage.getChat().getId());
        }
        return chatMessageDto;
    }
}