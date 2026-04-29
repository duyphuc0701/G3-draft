package com.example.g3.client;

import com.example.g3.dto.IdvVerifyRequest;
import com.example.g3.dto.IdvVerifyResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.CompletableFuture;

@Component
public class IdvClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public IdvClient(RestTemplate restTemplate, @Value("${idv.service.url:http://localhost:8083}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    @TimeLimiter(name = "idv")
    @CircuitBreaker(name = "idv")
    @Retry(name = "idv")
    public CompletableFuture<IdvVerifyResponse> triggerVerification(String correlationId, String idempotencyKey, IdvVerifyRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Correlation-Id", correlationId);
            headers.set("X-Idempotency-Key", idempotencyKey);
            HttpEntity<IdvVerifyRequest> entity = new HttpEntity<>(request, headers);

            try {
                ResponseEntity<IdvVerifyResponse> response = restTemplate.exchange(
                        baseUrl + "/idv/verify", HttpMethod.POST, entity, IdvVerifyResponse.class);
                return response.getBody();
            } catch (HttpStatusCodeException e) {
                if (e.getStatusCode().value() == 429 || e.getStatusCode().value() == 503) {
                    throw new IdvRetryableException("Retryable error: " + e.getStatusCode());
                }
                throw new IdvNonRetryableException("Non-retryable error: " + e.getStatusCode());
            }
        });
    }
}
