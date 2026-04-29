package com.example.g3.dto;

import java.util.UUID;

public class CreateSessionResponse {
    private UUID sessionId;

    public CreateSessionResponse() {}

    public CreateSessionResponse(UUID sessionId) {
        this.sessionId = sessionId;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public void setSessionId(UUID sessionId) {
        this.sessionId = sessionId;
    }
}
