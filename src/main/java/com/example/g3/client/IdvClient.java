package com.example.g3.client;

import com.example.g3.dto.IdvVerifyRequest;
import com.example.g3.dto.IdvVerifyResponse;
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
public class IdvClient {

    private final WebClient webClient;

    public IdvClient(@Qualifier("idvWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    @TimeLimiter(name = "idv")
    @CircuitBreaker(name = "idv")
    @Retry(name = "idv")
    public CompletableFuture<IdvVerifyResponse> triggerVerification(String correlationId, String idempotencyKey, IdvVerifyRequest request) {
        return webClient.post()
                .uri("/idv/verify")
                .header("X-Correlation-Id", correlationId)
                .header("X-Idempotency-Key", idempotencyKey)
                .bodyValue(request)
                .retrieve()
                .onStatus(status -> status.value() == 429 || status.value() == 503, this::retryableError)
                .onStatus(status -> status.isError(), this::nonRetryableError)
                .bodyToMono(IdvVerifyResponse.class)
                .toFuture();
    }

    private Mono<? extends Throwable> retryableError(ClientResponse response) {
        return Mono.just(new IdvRetryableException("Retryable error: " + response.statusCode()));
    }

    private Mono<? extends Throwable> nonRetryableError(ClientResponse response) {
        return Mono.just(new IdvNonRetryableException("Non-retryable error: " + response.statusCode()));
    }
}
