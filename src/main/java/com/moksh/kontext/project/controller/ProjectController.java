package com.moksh.kontext.project.controller;

import com.moksh.kontext.common.response.ApiResponse;
import com.moksh.kontext.common.response.PageResponse;
import com.moksh.kontext.project.dto.CreateProjectDto;
import com.moksh.kontext.project.dto.ProjectDto;
import com.moksh.kontext.project.dto.UpdateProjectDto;
import com.moksh.kontext.project.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
@Slf4j
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    public ApiResponse<ProjectDto> createProject(@Valid @RequestBody CreateProjectDto createProjectDto) {
        log.debug("POST /api/projects - Creating new project");
        
        ProjectDto createdProject = projectService.createProject(createProjectDto);
        return ApiResponse.success(createdProject, "Project created successfully", 201);
    }

    @GetMapping
    public ApiResponse<List<ProjectDto>> getUserProjects() {
        log.debug("GET /api/projects - Fetching user projects");
        
        List<ProjectDto> projects = projectService.getUserProjects();
        return ApiResponse.success(projects, "Projects retrieved successfully");
    }

    @GetMapping("/paginated")
    public ApiResponse<PageResponse<ProjectDto>> getUserProjectsPaginated(@PageableDefault(size = 10) Pageable pageable) {
        log.debug("GET /api/projects/paginated - Fetching user projects paginated");
        
        Page<ProjectDto> projects = projectService.getUserProjects(pageable);
        PageResponse<ProjectDto> pageResponse = PageResponse.of(projects);
        
        return ApiResponse.success(pageResponse, "Projects retrieved successfully");
    }

    @GetMapping("/{id}")
    public ApiResponse<ProjectDto> getProjectById(@PathVariable UUID id) {
        log.debug("GET /api/projects/{} - Fetching project by id", id);
        
        ProjectDto project = projectService.getProjectById(id);
        return ApiResponse.success(project, "Project retrieved successfully");
    }

    @PutMapping("/{id}")
    public ApiResponse<ProjectDto> updateProject(@PathVariable UUID id, @Valid @RequestBody UpdateProjectDto updateProjectDto) {
        log.debug("PUT /api/projects/{} - Updating project", id);
        
        ProjectDto updatedProject = projectService.updateProject(id, updateProjectDto);
        return ApiResponse.success(updatedProject, "Project updated successfully");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteProject(@PathVariable UUID id) {
        log.debug("DELETE /api/projects/{} - Deleting project", id);
        
        projectService.deleteProject(id);
        return ApiResponse.success(null, "Project deleted successfully", 204);
    }

    @GetMapping("/search")
    public ApiResponse<PageResponse<ProjectDto>> searchProjects(
            @RequestParam String searchTerm,
            @PageableDefault(size = 10) Pageable pageable) {
        log.debug("GET /api/projects/search - Searching projects with term: {}", searchTerm);
        
        Page<ProjectDto> projects = projectService.searchProjects(searchTerm, pageable);
        PageResponse<ProjectDto> pageResponse = PageResponse.of(projects);
        
        return ApiResponse.success(pageResponse, "Projects searched successfully");
    }
}