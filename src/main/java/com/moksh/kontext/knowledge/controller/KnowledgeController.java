package com.moksh.kontext.knowledge.controller;

import com.moksh.kontext.common.response.ApiResponse;
import com.moksh.kontext.common.response.PageResponse;
import com.moksh.kontext.knowledge.dto.CreateKnowledgeDto;
import com.moksh.kontext.knowledge.dto.KnowledgeDto;
import com.moksh.kontext.knowledge.entity.Knowledge;
import com.moksh.kontext.knowledge.service.KnowledgeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/projects/{projectId}/knowledge")
@RequiredArgsConstructor
@Slf4j
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    @PostMapping("/upload")
    public ApiResponse<KnowledgeDto> uploadFileKnowledge(
            @PathVariable UUID projectId,
            @RequestParam("file") MultipartFile file) {
        log.debug("POST /projects/{}/knowledge/upload - Uploading file knowledge", projectId);
        
        KnowledgeDto createdKnowledge = knowledgeService.uploadFileKnowledge(projectId, file);
        return ApiResponse.success(createdKnowledge, "File knowledge uploaded successfully", 201);
    }

    @PostMapping("/web")
    public ApiResponse<KnowledgeDto> createWebKnowledge(
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateKnowledgeDto createKnowledgeDto) {
        log.debug("POST /projects/{}/knowledge/web - Creating web knowledge", projectId);
        
        KnowledgeDto createdKnowledge = knowledgeService.createWebKnowledge(projectId, createKnowledgeDto);
        return ApiResponse.success(createdKnowledge, "Web knowledge created successfully", 201);
    }

    @GetMapping
    public ApiResponse<List<KnowledgeDto>> getProjectKnowledge(@PathVariable UUID projectId) {
        log.debug("GET /projects/{}/knowledge - Fetching project knowledge", projectId);
        
        List<KnowledgeDto> knowledgeItems = knowledgeService.getProjectKnowledge(projectId);
        return ApiResponse.success(knowledgeItems, "Knowledge items retrieved successfully");
    }

    @GetMapping("/paginated")
    public ApiResponse<PageResponse<KnowledgeDto>> getProjectKnowledgePaginated(
            @PathVariable UUID projectId,
            @PageableDefault(size = 10) Pageable pageable) {
        log.debug("GET /projects/{}/knowledge/paginated - Fetching project knowledge paginated", projectId);
        
        Page<KnowledgeDto> knowledgeItems = knowledgeService.getProjectKnowledge(projectId, pageable);
        PageResponse<KnowledgeDto> pageResponse = PageResponse.of(knowledgeItems);
        
        return ApiResponse.success(pageResponse, "Knowledge items retrieved successfully");
    }

    @GetMapping("/type/{type}")
    public ApiResponse<List<KnowledgeDto>> getProjectKnowledgeByType(
            @PathVariable UUID projectId,
            @PathVariable Knowledge.KnowledgeType type) {
        log.debug("GET /projects/{}/knowledge/type/{} - Fetching knowledge by type", projectId, type);
        
        List<KnowledgeDto> knowledgeItems = knowledgeService.getProjectKnowledgeByType(projectId, type);
        return ApiResponse.success(knowledgeItems, "Knowledge items retrieved successfully");
    }

    @GetMapping("/{knowledgeId}")
    public ApiResponse<KnowledgeDto> getKnowledgeById(
            @PathVariable UUID projectId,
            @PathVariable UUID knowledgeId) {
        log.debug("GET /projects/{}/knowledge/{} - Fetching knowledge by id", projectId, knowledgeId);
        
        KnowledgeDto knowledge = knowledgeService.getKnowledgeById(projectId, knowledgeId);
        return ApiResponse.success(knowledge, "Knowledge retrieved successfully");
    }

    @DeleteMapping("/{knowledgeId}")
    public ApiResponse<Void> deleteKnowledge(
            @PathVariable UUID projectId,
            @PathVariable UUID knowledgeId) {
        log.debug("DELETE /projects/{}/knowledge/{} - Deleting knowledge", projectId, knowledgeId);
        
        knowledgeService.deleteKnowledge(projectId, knowledgeId);
        return ApiResponse.success(null, "Knowledge deleted successfully", 204);
    }

    @GetMapping("/search")
    public ApiResponse<PageResponse<KnowledgeDto>> searchKnowledge(
            @PathVariable UUID projectId,
            @RequestParam String searchTerm,
            @PageableDefault(size = 10) Pageable pageable) {
        log.debug("GET /projects/{}/knowledge/search - Searching knowledge with term: {}", projectId, searchTerm);
        
        Page<KnowledgeDto> knowledgeItems = knowledgeService.searchKnowledge(projectId, searchTerm, pageable);
        PageResponse<KnowledgeDto> pageResponse = PageResponse.of(knowledgeItems);
        
        return ApiResponse.success(pageResponse, "Knowledge search completed successfully");
    }
}