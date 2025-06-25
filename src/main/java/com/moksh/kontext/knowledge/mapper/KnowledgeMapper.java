package com.moksh.kontext.knowledge.mapper;

import com.moksh.kontext.knowledge.dto.KnowledgeDto;
import com.moksh.kontext.knowledge.entity.Knowledge;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class KnowledgeMapper {

    @Autowired
    private ModelMapper modelMapper;

    public KnowledgeDto toDto(Knowledge knowledge) {
        return modelMapper.map(knowledge, KnowledgeDto.class);
    }
}