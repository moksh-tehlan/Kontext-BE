package com.moksh.kontext.project.service;

import com.moksh.kontext.common.util.SecurityContextUtil;
import com.moksh.kontext.project.dto.CreateProjectDto;
import com.moksh.kontext.project.dto.ProjectDto;
import com.moksh.kontext.project.dto.UpdateProjectDto;
import com.moksh.kontext.project.entity.Project;
import com.moksh.kontext.project.exception.ProjectCreationFailedException;
import com.moksh.kontext.project.exception.ProjectDeleteFailedException;
import com.moksh.kontext.project.exception.ProjectNotFoundException;
import com.moksh.kontext.project.exception.ProjectUpdateFailedException;
import com.moksh.kontext.project.mapper.ProjectMapper;
import com.moksh.kontext.project.repository.ProjectRepository;
import com.moksh.kontext.user.entity.User;
import com.moksh.kontext.user.exception.UserNotFoundException;
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
        try {
            UUID currentUserId = SecurityContextUtil.getCurrentUserId();
            User user = userRepository.findById(currentUserId)
                    .orElseThrow(() -> UserNotFoundException.forId(currentUserId.toString()));

            Project project = projectMapper.toEntity(createProjectDto);
            project.setUser(user);
            
            Project savedProject = projectRepository.save(project);
            return projectMapper.toDto(savedProject);
        } catch (Exception e) {
            if (e instanceof UserNotFoundException) {
                throw e;
            }
            throw ProjectCreationFailedException.withReason(e.getMessage());
        }
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
                .orElseThrow(() -> ProjectNotFoundException.forUserAndId(currentUserId.toString(), projectId.toString()));
        
        return projectMapper.toDto(project);
    }

    public ProjectDto updateProject(UUID projectId, UpdateProjectDto updateProjectDto) {
        try {
            UUID currentUserId = SecurityContextUtil.getCurrentUserId();
            Project project = projectRepository.findByIdAndUserIdAndIsActiveTrue(projectId, currentUserId)
                    .orElseThrow(() -> ProjectNotFoundException.forUserAndId(currentUserId.toString(), projectId.toString()));

            projectMapper.updateEntityFromDto(updateProjectDto, project);
            Project updatedProject = projectRepository.save(project);
            
            return projectMapper.toDto(updatedProject);
        } catch (Exception e) {
            if (e instanceof ProjectNotFoundException) {
                throw e;
            }
            throw ProjectUpdateFailedException.withReason(projectId.toString(), e.getMessage());
        }
    }

    public void deleteProject(UUID projectId) {
        try {
            UUID currentUserId = SecurityContextUtil.getCurrentUserId();
            Project project = projectRepository.findByIdAndUserIdAndIsActiveTrue(projectId, currentUserId)
                    .orElseThrow(() -> ProjectNotFoundException.forUserAndId(currentUserId.toString(), projectId.toString()));

            project.setIsActive(false);
            projectRepository.save(project);
        } catch (Exception e) {
            if (e instanceof ProjectNotFoundException) {
                throw e;
            }
            throw ProjectDeleteFailedException.withReason(projectId.toString(), e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Page<ProjectDto> searchProjects(String searchTerm, Pageable pageable) {
        UUID currentUserId = SecurityContextUtil.getCurrentUserId();
        Page<Project> projects = projectRepository.findByUserIdAndSearchTerm(currentUserId, searchTerm, pageable);
        
        return projects.map(projectMapper::toDto);
    }
}