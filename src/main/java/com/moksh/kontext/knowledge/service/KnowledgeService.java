package com.moksh.kontext.knowledge.service;

import com.moksh.kontext.ai.service.VectorService;
import com.moksh.kontext.aws.service.S3Service;
import com.moksh.kontext.common.exception.ResourceNotFoundException;
import com.moksh.kontext.common.util.SecurityContextUtil;
import com.moksh.kontext.knowledge.dto.CreateKnowledgeDto;
import com.moksh.kontext.knowledge.dto.KnowledgeDto;
import com.moksh.kontext.knowledge.entity.Knowledge;
import com.moksh.kontext.common.exception.InvalidWebUrlException;
import com.moksh.kontext.common.exception.UnsupportedFileTypeException;
import com.moksh.kontext.knowledge.mapper.KnowledgeMapper;
import com.moksh.kontext.knowledge.repository.KnowledgeRepository;
import com.moksh.kontext.project.entity.Project;
import com.moksh.kontext.project.repository.ProjectRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class KnowledgeService {

    @Autowired
    private KnowledgeRepository knowledgeRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private KnowledgeMapper knowledgeMapper;

    @Autowired
    private S3Service s3Service;
    @Autowired
    private VectorService vectorService;

    public KnowledgeDto uploadFileKnowledge(UUID projectId, MultipartFile file) {
        UUID currentUserId = SecurityContextUtil.getCurrentUserId();
        
        Project project = projectRepository.findByIdAndUserIdAndIsActiveTrue(projectId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        String contentType = file.getContentType();
        Knowledge.KnowledgeType knowledgeType = determineKnowledgeType(contentType);
        
        if (knowledgeType == Knowledge.KnowledgeType.WEB) {
            throw new UnsupportedFileTypeException("File upload not supported for WEB type. Use web URL instead.");
        }

        String s3Url = s3Service.uploadFile(file, getS3Folder(knowledgeType));
        String fileName = extractFileName(file.getOriginalFilename());

        Knowledge knowledge = new Knowledge();
        knowledge.setName(fileName);
        knowledge.setType(knowledgeType);
        knowledge.setMimeType(contentType);
        knowledge.setSize(file.getSize());
        knowledge.setSource(s3Url);
        knowledge.setProject(project);
        
        Knowledge savedKnowledge = knowledgeRepository.save(knowledge);

        // create dummy documents for vectorstore
        List<Document> documents = createDummyDocuments(savedKnowledge.getId());

        vectorService.addDocuments(documents);
        log.info("File knowledge created: {} for project: {}", fileName, projectId);
        
        return knowledgeMapper.toDto(savedKnowledge);
    }

    public KnowledgeDto createWebKnowledge(UUID projectId, CreateKnowledgeDto createKnowledgeDto) {
        UUID currentUserId = SecurityContextUtil.getCurrentUserId();
        
        Project project = projectRepository.findByIdAndUserIdAndIsActiveTrue(projectId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        validateWebUrl(createKnowledgeDto.getWebUrl());
        String urlName = extractUrlName(createKnowledgeDto.getWebUrl());

        Knowledge knowledge = new Knowledge();
        knowledge.setName(urlName);
        knowledge.setType(Knowledge.KnowledgeType.WEB);
        knowledge.setMimeType("text/html");
        knowledge.setSize(null);
        knowledge.setSource(createKnowledgeDto.getWebUrl());
        knowledge.setProject(project);
        
        Knowledge savedKnowledge = knowledgeRepository.save(knowledge);
        log.info("Web knowledge created: {} for project: {}", urlName, projectId);
        
        return knowledgeMapper.toDto(savedKnowledge);
    }

    @Transactional(readOnly = true)
    public List<KnowledgeDto> getProjectKnowledge(UUID projectId) {
        UUID currentUserId = SecurityContextUtil.getCurrentUserId();
        
        projectRepository.findByIdAndUserIdAndIsActiveTrue(projectId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        List<Knowledge> knowledgeItems = knowledgeRepository.findByProjectIdAndIsActiveTrue(projectId);
        
        return knowledgeItems.stream()
                .map(knowledgeMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<KnowledgeDto> getProjectKnowledge(UUID projectId, Pageable pageable) {
        UUID currentUserId = SecurityContextUtil.getCurrentUserId();
        
        projectRepository.findByIdAndUserIdAndIsActiveTrue(projectId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        Page<Knowledge> knowledgeItems = knowledgeRepository.findByProjectIdAndIsActiveTrue(projectId, pageable);
        
        return knowledgeItems.map(knowledgeMapper::toDto);
    }

    @Transactional(readOnly = true)
    public List<KnowledgeDto> getProjectKnowledgeByType(UUID projectId, Knowledge.KnowledgeType type) {
        UUID currentUserId = SecurityContextUtil.getCurrentUserId();
        
        projectRepository.findByIdAndUserIdAndIsActiveTrue(projectId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        List<Knowledge> knowledgeItems = knowledgeRepository.findByProjectIdAndTypeAndIsActiveTrue(projectId, type);
        
        return knowledgeItems.stream()
                .map(knowledgeMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public KnowledgeDto getKnowledgeById(UUID projectId, UUID knowledgeId) {
        UUID currentUserId = SecurityContextUtil.getCurrentUserId();
        
        projectRepository.findByIdAndUserIdAndIsActiveTrue(projectId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        Knowledge knowledge = knowledgeRepository.findByIdAndProjectIdAndIsActiveTrue(knowledgeId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Knowledge not found"));
        
        return knowledgeMapper.toDto(knowledge);
    }

    public void deleteKnowledge(UUID projectId, UUID knowledgeId) {
        UUID currentUserId = SecurityContextUtil.getCurrentUserId();
        
        projectRepository.findByIdAndUserIdAndIsActiveTrue(projectId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        Knowledge knowledge = knowledgeRepository.findByIdAndProjectIdAndIsActiveTrue(knowledgeId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Knowledge not found"));

        if (knowledge.getType() != Knowledge.KnowledgeType.WEB) {
            try {
                s3Service.deleteFile(knowledge.getSource());
            } catch (Exception e) {
                log.warn("Failed to delete file from S3: {}", knowledge.getSource(), e);
            }
        }

        knowledge.setIsActive(false);
        knowledgeRepository.save(knowledge);
        log.info("Knowledge deleted: {} from project: {}", knowledge.getName(), projectId);
    }

    @Transactional(readOnly = true)
    public Page<KnowledgeDto> searchKnowledge(UUID projectId, String searchTerm, Pageable pageable) {
        UUID currentUserId = SecurityContextUtil.getCurrentUserId();
        
        projectRepository.findByIdAndUserIdAndIsActiveTrue(projectId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        Page<Knowledge> knowledgeItems = knowledgeRepository.findByProjectIdAndSearchTerm(projectId, searchTerm, pageable);
        
        return knowledgeItems.map(knowledgeMapper::toDto);
    }

    private Knowledge.KnowledgeType determineKnowledgeType(String contentType) {
        if (isImageFile(contentType)) {
            return Knowledge.KnowledgeType.IMAGE;
        } else if (isDocumentFile(contentType)) {
            return Knowledge.KnowledgeType.DOCUMENT;
        } else {
            throw new UnsupportedFileTypeException("Unsupported file type: " + contentType);
        }
    }

    private boolean isImageFile(String contentType) {
        return contentType != null && contentType.startsWith("image/");
    }

    private boolean isDocumentFile(String contentType) {
        return contentType != null && (
            contentType.equals("application/pdf") ||
            contentType.equals("application/msword") ||
            contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document") ||
            contentType.equals("application/vnd.ms-powerpoint") ||
            contentType.equals("application/vnd.openxmlformats-officedocument.presentationml.presentation") ||
            contentType.equals("text/plain") ||
            contentType.equals("application/rtf") ||
            contentType.equals("application/vnd.ms-excel") ||
            contentType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        );
    }

    private String getS3Folder(Knowledge.KnowledgeType type) {
        return switch (type) {
            case IMAGE -> "images";
            case DOCUMENT -> "documents";
            case WEB -> throw new UnsupportedFileTypeException("WEB type does not use S3 storage");
        };
    }

    private String extractFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.trim().isEmpty()) {
            return "unknown_file";
        }
        return originalFileName.trim();
    }

    private String extractUrlName(String url) {
        try {
            URL urlObj = new URL(url);
            String host = urlObj.getHost();
            return host != null ? host : "web_resource";
        } catch (Exception e) {
            return "web_resource";
        }
    }

    private void validateWebUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new InvalidWebUrlException("Web URL cannot be empty");
        }
        
        try {
            new URL(url);
        } catch (Exception e) {
            throw new InvalidWebUrlException("Invalid web URL format: " + url, e);
        }
    }

    public static List<Document> createDummyDocuments(UUID knowledgeId) {
        // Dummy Project IDs for testing filters

        return List.of(
                // Document 1: Related to Project 1, focusing on "Spring Boot Microservices"
                new Document(
                        "Microservices architecture with Spring Boot empowers developers to build scalable, resilient, and independently deployable services. Key components include Eureka for service discovery, Feign for declarative REST clients, and Resilience4j for circuit breaking.  For data, developers often use Spring Data JPA with PostgreSQL, and for messaging, Apache Kafka is a popular choice.",
                        Map.of(
                                "knowledge_id", knowledgeId,
                                "title", "Introduction to Spring Boot Microservices",
                                "author", "Alice Smith",
                                "version", "1.0",
                                "source", "internal_documentation"
                        )
                ),
                // Document 2: Related to Project 1, focusing on "Spring Security"
                new Document(
                        "Spring Security provides comprehensive security services for Java EE-based enterprise software applications. It offers authentication and authorization support, protecting your application from common vulnerabilities. OAuth2 and JWT (JSON Web Tokens) are frequently used for secure API communication. Implementing a custom UserDetailsService is common for integrating with existing user stores.",
                        Map.of(
                                "knowledge_id", knowledgeId,
                                "title", "Securing Applications with Spring Security",
                                "author", "Bob Johnson",
                                "version", "1.2",
                                "source", "company_wiki"
                        )
                ),
                // Document 3: Related to Project 2, focusing on "Cloud Deployment (AWS)"
                new Document(
                        "Deploying Spring Boot applications to AWS involves leveraging services like EC2 for compute, RDS for managed databases (e.g., PostgreSQL, MySQL), and S3 for object storage. For CI/CD, AWS CodePipeline and CodeDeploy are often used. Containerization with Docker and orchestration with EKS (Elastic Kubernetes Service) or ECS (Elastic Container Service) are common deployment strategies.",
                        Map.of(
                                "knowledge_id", knowledgeId,
                                "title", "AWS Deployment Strategies for Spring Boot",
                                "author", "Charlie Brown",
                                "version", "2.0",
                                "source", "dev_blog"
                        )
                ),
                // Document 4: Related to Project 2, focusing on "Testing in Spring"
                new Document(
                        "Effective testing in Spring applications involves unit, integration, and end-to-end tests. Spring Boot provides excellent support for testing with `@SpringBootTest`, `@WebMvcTest`, and `@DataJpaTest`. Mockito is widely used for mocking dependencies, and JUnit 5 is the standard testing framework. Property-based testing can also be explored for robust test suites.",
                        Map.of(
                                "knowledge_id", knowledgeId,
                                "title", "Comprehensive Testing in Spring Boot",
                                "author", "Alice Smith",
                                "version", "1.1",
                                "source", "internal_training_material"
                        )
                ),
                // Document 5: Related to Project 3, but less relevant to typical Spring context
                new Document(
                        "The principles of agile software development emphasize iterative development, collaboration, and responding to change. Scrum is a popular agile framework that defines roles, events, and artifacts to deliver value incrementally. Daily stand-ups, sprint planning, and sprint reviews are key ceremonies.",
                        Map.of(
                                "knowledge_id", knowledgeId,
                                "title", "Understanding Agile Methodologies",
                                "author", "David Lee",
                                "version", "1.0",
                                "source", "external_article"
                        )
                ),
                // Document 6: Another document for Project 1, on a slightly different topic
                new Document(
                        "Logging and monitoring are critical for production systems. In Spring Boot, Logback is the default logging framework, and metrics can be exposed via Micrometer and viewed with Prometheus and Grafana. Distributed tracing with Brave and Zipkin helps in debugging microservices by visualizing request flows across services.",
                        Map.of(
                                "knowledge_id", knowledgeId,
                                "title", "Logging and Monitoring Spring Boot Applications",
                                "author", "Alice Smith",
                                "version", "1.0",
                                "source", "internal_documentation"
                        )
                )
        );
    }

}