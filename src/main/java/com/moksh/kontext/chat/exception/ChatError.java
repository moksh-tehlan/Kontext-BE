package com.moksh.kontext.chat.exception;

import com.moksh.kontext.common.error.BaseAppError;

public class ChatError extends BaseAppError {
    
    private static final String MODULE = "CHAT";
    
    // Private constructor for creating chat error instances
    private ChatError(String errorCode, int statusCode, String defaultMessage) {
        super(errorCode, statusCode, MODULE, defaultMessage);
    }
    
    // Message Related Errors (7000-7099)
    public static final ChatError MESSAGE_NOT_FOUND = new ChatError("CHAT_MESSAGE_NOT_FOUND", 7001, "Message not found");
    public static final ChatError MESSAGE_SEND_FAILED = new ChatError("CHAT_MESSAGE_SEND_FAILED", 7002, "Failed to send message");
    public static final ChatError MESSAGE_TOO_LONG = new ChatError("CHAT_MESSAGE_TOO_LONG", 7003, "Message exceeds maximum length");
    public static final ChatError INVALID_MESSAGE_FORMAT = new ChatError("CHAT_INVALID_MESSAGE_FORMAT", 7004, "Invalid message format");
    
    // Conversation Related Errors (7100-7199)
    public static final ChatError CONVERSATION_NOT_FOUND = new ChatError("CHAT_CONVERSATION_NOT_FOUND", 7101, "Conversation not found");
    public static final ChatError CONVERSATION_ACCESS_DENIED = new ChatError("CHAT_CONVERSATION_ACCESS_DENIED", 7102, "Access denied to conversation");
    public static final ChatError CONVERSATION_CREATE_FAILED = new ChatError("CHAT_CONVERSATION_CREATE_FAILED", 7103, "Failed to create conversation");
    
    // Chat Room Related Errors (7200-7299)
    public static final ChatError CHAT_ROOM_FULL = new ChatError("CHAT_ROOM_FULL", 7201, "Chat room is full");
    public static final ChatError CHAT_ROOM_CLOSED = new ChatError("CHAT_ROOM_CLOSED", 7202, "Chat room is closed");
    public static final ChatError USER_BANNED_FROM_CHAT = new ChatError("CHAT_USER_BANNED", 7203, "User is banned from this chat");
}