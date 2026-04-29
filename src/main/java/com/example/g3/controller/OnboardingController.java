package com.example.g3.controller;

import com.example.g3.dto.CreateSessionResponse;
import com.example.g3.dto.DocumentsRequest;
import com.example.g3.dto.PersonalDetailsRequest;
import com.example.g3.dto.StatusResponse;
import com.example.g3.service.OnboardingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/onboarding")
public class OnboardingController {

    private final OnboardingService service;

    public OnboardingController(OnboardingService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<CreateSessionResponse> startOnboarding() {
        UUID sessionId = service.startOnboarding();
        return ResponseEntity.status(HttpStatus.CREATED).body(new CreateSessionResponse(sessionId));
    }

    @PutMapping("/{sessionId}/personal-details")
    public ResponseEntity<Void> updatePersonalDetails(@PathVariable UUID sessionId, @RequestBody PersonalDetailsRequest request) {
        service.updatePersonalDetails(sessionId, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{sessionId}/documents")
    public ResponseEntity<Void> uploadDocuments(@PathVariable UUID sessionId, @RequestBody DocumentsRequest request) {
        service.uploadDocuments(sessionId, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{sessionId}/verify")
    public ResponseEntity<Void> verify(@PathVariable UUID sessionId) {
        // Run verify logic (which updates status to VERIFICATION_PENDING then ACCOUNT_CREATION_PENDING)
        service.verify(sessionId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @GetMapping("/{sessionId}/status")
    public ResponseEntity<StatusResponse> getStatus(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(new StatusResponse(service.getStatus(sessionId)));
    }
}
