package com.moksh.kontext.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateChatDto {

    @NotBlank(message = "Chat name is required")
    @Size(max = 100, message = "Chat name must not exceed 100 characters")
    private String name;
}