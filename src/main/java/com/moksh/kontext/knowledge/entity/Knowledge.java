package com.moksh.kontext.knowledge.entity;

import com.moksh.kontext.common.entity.BaseEntity;
import com.moksh.kontext.project.entity.Project;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "knowledge")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Knowledge extends BaseEntity {

    @NotBlank(message = "Knowledge name is required")
    @Size(max = 255, message = "Knowledge name must not exceed 255 characters")
    @Column(name = "name", nullable = false)
    private String name;

    @NotNull(message = "Knowledge type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private KnowledgeType type;

    @Size(max = 100, message = "MIME type must not exceed 100 characters")
    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "size")
    private Long size;

    @NotBlank(message = "Knowledge source is required")
    @Size(max = 2000, message = "Source must not exceed 2000 characters")
    @Column(name = "source", nullable = false, length = 2000)
    private String source;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @NotNull(message = "Processing status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false)
    private ProcessingStatus processingStatus = ProcessingStatus.PROCESSING;

    @Size(max = 1000, message = "Error details must not exceed 1000 characters")
    @Column(name = "error_details", length = 1000)
    private String errorDetails;

    public enum KnowledgeType {
        IMAGE,
        DOCUMENT,
        WEB
    }

    public enum ProcessingStatus {
        PROCESSING,
        SUCCESS,
        FAILED
    }
}