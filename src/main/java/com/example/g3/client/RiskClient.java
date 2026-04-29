package com.example.g3.client;

import com.example.g3.dto.RiskComplianceRequest;
import com.example.g3.dto.RiskComplianceResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.CompletableFuture;

@Component
public class RiskClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public RiskClient(@Qualifier("riskRestTemplate") RestTemplate restTemplate, @Value("${risk.service.url:http://localhost:8084}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    @TimeLimiter(name = "risk")
    @CircuitBreaker(name = "risk")
    @Retry(name = "risk")
    public CompletableFuture<RiskComplianceResponse> performComplianceCheck(String correlationId, String idempotencyKey, RiskComplianceRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Correlation-Id", correlationId);
            headers.set("X-Idempotency-Key", idempotencyKey);
            HttpEntity<RiskComplianceRequest> entity = new HttpEntity<>(request, headers);

            try {
                ResponseEntity<RiskComplianceResponse> response = restTemplate.exchange(
                        baseUrl + "/compliance/check", HttpMethod.POST, entity, RiskComplianceResponse.class);
                return response.getBody();
            } catch (HttpStatusCodeException e) {
                if (e.getStatusCode().value() == 429 || e.getStatusCode().value() == 503) {
                    throw new RiskRetryableException("Retryable compliance error: " + e.getStatusCode());
                }
                throw new RiskNonRetryableException("Non-retryable compliance error: " + e.getStatusCode());
            }
        });
    }
}
