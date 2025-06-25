package com.moksh.kontext.project.mapper;

import com.moksh.kontext.project.dto.CreateProjectDto;
import com.moksh.kontext.project.dto.ProjectDto;
import com.moksh.kontext.project.dto.UpdateProjectDto;
import com.moksh.kontext.project.entity.Project;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ProjectMapper {

    @Autowired
    private ModelMapper modelMapper;

    public ProjectDto toDto(Project project) {
        return modelMapper.map(project, ProjectDto.class);
    }

    public Project toEntity(CreateProjectDto createProjectDto) {
        return modelMapper.map(createProjectDto, Project.class);
    }

    public void updateEntityFromDto(UpdateProjectDto updateProjectDto, Project project) {
        if (updateProjectDto.getName() != null) {
            project.setName(updateProjectDto.getName());
        }
        if (updateProjectDto.getDescription() != null) {
            project.setDescription(updateProjectDto.getDescription());
        }
    }
}