package com.moksh.kontext.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateChatDto {

    @NotBlank(message = "Chat name is required")
    @Size(max = 100, message = "Chat name must not exceed 100 characters")
    private String name;

    @NotNull(message = "Project ID is required")
    private UUID projectId;
}