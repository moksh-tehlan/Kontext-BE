package com.moksh.kontext.knowledge_processing.dto.event;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.moksh.kontext.knowledge_processing.constants.EventType;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Data
@SuperBuilder
@NoArgsConstructor
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME, 
    property = "eventType",
    defaultImpl = ContentProcessRequestEvent.class,
    visible = true
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = ContentProcessRequestEvent.class, name = EventType.CONTENT_PROCESS_REQUEST),
    @JsonSubTypes.Type(value = ContentProcessSuccessEvent.class, name = EventType.CONTENT_PROCESS_SUCCESS),
    @JsonSubTypes.Type(value = ContentProcessFailedEvent.class, name = EventType.CONTENT_PROCESS_FAILED)
})
public abstract class ContentProcessEvent {
    
    private String eventId;
    private String eventType;
    private LocalDateTime timestamp;
    private String contentId;
    private String contentType; // "document", "image", "web"
}