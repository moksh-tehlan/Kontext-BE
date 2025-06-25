package com.moksh.kontext.knowledge.dto;

import com.moksh.kontext.common.dto.BaseDto;
import com.moksh.kontext.knowledge.entity.Knowledge;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeDto extends BaseDto {

    private String name;
    private Knowledge.KnowledgeType type;
    private String mimeType;
    private Long size;
    private String source;
    private UUID projectId;
}