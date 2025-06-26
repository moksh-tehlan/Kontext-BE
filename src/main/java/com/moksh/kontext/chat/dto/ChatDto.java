package com.moksh.kontext.chat.dto;

import com.moksh.kontext.common.dto.BaseDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatDto extends BaseDto {

    private String name;
    private UUID projectId;
}