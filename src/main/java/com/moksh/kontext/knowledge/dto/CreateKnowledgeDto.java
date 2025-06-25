package com.moksh.kontext.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
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
public class CreateKnowledgeDto {

    @NotBlank(message = "Web URL is required for WEB type knowledge")
    @Size(max = 2000, message = "URL must not exceed 2000 characters")
    private String webUrl;

    private UUID projectId;
}