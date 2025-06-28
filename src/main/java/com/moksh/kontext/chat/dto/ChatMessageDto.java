package com.moksh.kontext.chat.dto;

import com.moksh.kontext.common.dto.BaseDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.ai.chat.messages.MessageType;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDto extends BaseDto {

    private UUID chatId;
    private String content;
    private MessageType type;
}