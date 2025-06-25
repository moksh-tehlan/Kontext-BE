package com.moksh.kontext.project.service;

import com.moksh.kontext.common.exception.ResourceNotFoundException;
import com.moksh.kontext.common.util.SecurityContextUtil;
import com.moksh.kontext.project.dto.CreateProjectDto;
import com.moksh.kontext.project.dto.ProjectDto;
import com.moksh.kontext.project.dto.UpdateProjectDto;
import com.moksh.kontext.project.entity.Project;
import com.moksh.kontext.project.mapper.ProjectMapper;
import com.moksh.kontext.project.repository.ProjectRepository;
import com.moksh.kontext.user.entity.User;
import com.moksh.kontext.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProjectService {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectMapper projectMapper;

    public ProjectDto createProject(CreateProjectDto createProjectDto) {
        UUID currentUserId = SecurityContextUtil.getCurrentUserId();
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Project project = projectMapper.toEntity(createProjectDto);
        project.setUser(user);
        
        Project savedProject = projectRepository.save(project);
        return projectMapper.toDto(savedProject);
    }

    @Transactional(readOnly = true)
    public List<ProjectDto> getUserProjects() {
        UUID currentUserId = SecurityContextUtil.getCurrentUserId();
        List<Project> projects = projectRepository.findByUserIdAndIsActiveTrue(currentUserId);
        
        return projects.stream()
                .map(projectMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<ProjectDto> getUserProjects(Pageable pageable) {
        UUID currentUserId = SecurityContextUtil.getCurrentUserId();
        Page<Project> projects = projectRepository.findByUserIdAndIsActiveTrue(currentUserId, pageable);
        
        return projects.map(projectMapper::toDto);
    }

    @Transactional(readOnly = true)
    public ProjectDto getProjectById(UUID projectId) {
        UUID currentUserId = SecurityContextUtil.getCurrentUserId();
        Project project = projectRepository.findByIdAndUserIdAndIsActiveTrue(projectId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        
        return projectMapper.toDto(project);
    }

    public ProjectDto updateProject(UUID projectId, UpdateProjectDto updateProjectDto) {
        UUID currentUserId = SecurityContextUtil.getCurrentUserId();
        Project project = projectRepository.findByIdAndUserIdAndIsActiveTrue(projectId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        projectMapper.updateEntityFromDto(updateProjectDto, project);
        Project updatedProject = projectRepository.save(project);
        
        return projectMapper.toDto(updatedProject);
    }

    public void deleteProject(UUID projectId) {
        UUID currentUserId = SecurityContextUtil.getCurrentUserId();
        Project project = projectRepository.findByIdAndUserIdAndIsActiveTrue(projectId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        project.setIsActive(false);
        projectRepository.save(project);
    }

    @Transactional(readOnly = true)
    public Page<ProjectDto> searchProjects(String searchTerm, Pageable pageable) {
        UUID currentUserId = SecurityContextUtil.getCurrentUserId();
        Page<Project> projects = projectRepository.findByUserIdAndSearchTerm(currentUserId, searchTerm, pageable);
        
        return projects.map(projectMapper::toDto);
    }
}