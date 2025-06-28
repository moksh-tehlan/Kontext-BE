package com.moksh.kontext.knowledge_processing.constants;

public final class EventType {
    
    public static final String CONTENT_PROCESS_REQUEST = "content.process.request";
    public static final String CONTENT_PROCESS_SUCCESS = "content.process.success";
    public static final String CONTENT_PROCESS_FAILED = "content.process.failed";
    
    public static final String CONTENT_TYPE_DOCUMENT = "document";
    public static final String CONTENT_TYPE_IMAGE = "image";
    public static final String CONTENT_TYPE_WEB = "web";
    
    private EventType() {
    }
}