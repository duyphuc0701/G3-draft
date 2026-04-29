package com.example.g3.client;

import com.example.g3.dto.CisCustomerRequest;
import com.example.g3.dto.CisCustomerResponse;
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

@Component
public class CisClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public CisClient(@Qualifier("cisRestTemplate") RestTemplate restTemplate, @Value("${cis.service.url:http://localhost:8081}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    @CircuitBreaker(name = "cis")
    @Retry(name = "cis")
    public CisCustomerResponse createCustomer(String correlationId, String idempotencyKey, CisCustomerRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Correlation-Id", correlationId);
        headers.set("X-Idempotency-Key", idempotencyKey);
        HttpEntity<CisCustomerRequest> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<CisCustomerResponse> response = restTemplate.exchange(
                    baseUrl + "/customers", HttpMethod.POST, entity, CisCustomerResponse.class);
            return response.getBody();
        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode().value() == 429 || e.getStatusCode().value() == 503) {
                throw new CisRetryableException("Retryable error: " + e.getStatusCode());
            }
            throw new CisNonRetryableException("Non-retryable error: " + e.getStatusCode());
        }
    }
}
