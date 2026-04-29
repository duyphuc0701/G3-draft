package com.example.g3.client.auth;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class MockOAuth2TokenService {

    private String currentToken;
    private Instant expiresAt;

    public synchronized String getToken() {
        if (currentToken == null || Instant.now().isAfter(expiresAt)) {
            // Generate a dummy token that expires in 5 minutes
            currentToken = "mock-token-" + UUID.randomUUID().toString();
            expiresAt = Instant.now().plusSeconds(300);
        }
        return currentToken;
    }
}
