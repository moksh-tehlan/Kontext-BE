package com.moksh.kontext.project.dto;

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
public class ProjectDto extends BaseDto {

    private String name;
    private String description;
    private String agentInstruction;
    private UUID userId;
}