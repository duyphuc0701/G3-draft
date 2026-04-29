package com.example.g3.client;

import com.example.g3.dto.DocumentUploadRequest;
import com.example.g3.dto.DocumentUploadResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import java.util.concurrent.CompletableFuture;

@Component
public class DocumentClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public DocumentClient(@Qualifier("documentRestTemplate") RestTemplate restTemplate, @Value("${document.service.url:http://localhost:8082}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    @TimeLimiter(name = "document")
    @CircuitBreaker(name = "document")
    @Retry(name = "document")
    public CompletableFuture<DocumentUploadResponse> uploadDocument(String correlationId, String idempotencyKey, DocumentUploadRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Correlation-Id", correlationId);
            headers.set("X-Idempotency-Key", idempotencyKey);
            HttpEntity<DocumentUploadRequest> entity = new HttpEntity<>(request, headers);

            try {
                ResponseEntity<DocumentUploadResponse> response = restTemplate.exchange(
                        baseUrl + "/documents/upload", HttpMethod.POST, entity, DocumentUploadResponse.class);
                return response.getBody();
            } catch (HttpStatusCodeException e) {
                if (e.getStatusCode().value() == 503 || e.getStatusCode().value() == 429) {
                    throw new DocumentRetryableException("Retryable document error: " + e.getStatusCode());
                }
                throw new DocumentNonRetryableException("Non-retryable document error: " + e.getStatusCode());
            }
        });
    }
}
