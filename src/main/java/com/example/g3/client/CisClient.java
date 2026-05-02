package com.example.g3.client;

import com.example.g3.dto.CisCustomerRequest;
import com.example.g3.dto.CisCustomerResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class CisClient {

    private final WebClient webClient;

    public CisClient(@Qualifier("cisWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    @CircuitBreaker(name = "cis")
    @Retry(name = "cis")
    public CisCustomerResponse createCustomer(String correlationId, String idempotencyKey, CisCustomerRequest request) {
        return webClient.post()
                .uri("/customers")
                .header("X-Correlation-Id", correlationId)
                .header("X-Idempotency-Key", idempotencyKey)
                .bodyValue(request)
                .retrieve()
                .onStatus(status -> status.value() == 429 || status.value() == 503, this::retryableError)
                .onStatus(status -> status.isError(), this::nonRetryableError)
                .bodyToMono(CisCustomerResponse.class)
                .block();
    }

    private Mono<? extends Throwable> retryableError(ClientResponse response) {
        return Mono.just(new CisRetryableException("Retryable error: " + response.statusCode()));
    }

    private Mono<? extends Throwable> nonRetryableError(ClientResponse response) {
        return Mono.just(new CisNonRetryableException("Non-retryable error: " + response.statusCode()));
    }
}
