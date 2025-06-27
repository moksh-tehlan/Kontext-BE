package com.moksh.kontext.knowledge_processing.dto.event;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Data
@SuperBuilder
@NoArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "eventType")
@JsonSubTypes({
    @JsonSubTypes.Type(value = ContentProcessRequestEvent.class, name = "content.process.request"),
    @JsonSubTypes.Type(value = ContentProcessSuccessEvent.class, name = "content.process.success"),
    @JsonSubTypes.Type(value = ContentProcessFailedEvent.class, name = "content.process.failed")
})
public abstract class ContentProcessEvent {
    
    private String eventId;
    private String eventType;
    private LocalDateTime timestamp;
    private String contentId;
    private String contentType; // "document", "image", "web"
}