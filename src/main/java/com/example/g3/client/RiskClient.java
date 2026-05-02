package com.example.g3.client;

import com.example.g3.dto.RiskComplianceRequest;
import com.example.g3.dto.RiskComplianceResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;

@Component
public class RiskClient {

    private final WebClient webClient;

    public RiskClient(@Qualifier("riskWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    @TimeLimiter(name = "risk")
    @CircuitBreaker(name = "risk")
    @Retry(name = "risk")
    public CompletableFuture<RiskComplianceResponse> performComplianceCheck(String correlationId, String idempotencyKey, RiskComplianceRequest request) {
        return webClient.post()
                .uri("/compliance/check")
                .header("X-Correlation-Id", correlationId)
                .header("X-Idempotency-Key", idempotencyKey)
                .bodyValue(request)
                .retrieve()
                .onStatus(status -> status.value() == 429 || status.value() == 503, this::retryableError)
                .onStatus(status -> status.isError(), this::nonRetryableError)
                .bodyToMono(RiskComplianceResponse.class)
                .toFuture();
    }

    private Mono<? extends Throwable> retryableError(ClientResponse response) {
        return Mono.just(new RiskRetryableException("Retryable compliance error: " + response.statusCode()));
    }

    private Mono<? extends Throwable> nonRetryableError(ClientResponse response) {
        return Mono.just(new RiskNonRetryableException("Non-retryable compliance error: " + response.statusCode()));
    }
}
