package com.example.g3.client;

import com.example.g3.dto.DocumentUploadRequest;
import com.example.g3.dto.DocumentUploadResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;

@Component
public class DocumentClient {

    private final WebClient webClient;

    public DocumentClient(@Qualifier("documentWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    @TimeLimiter(name = "document")
    @CircuitBreaker(name = "document")
    @Retry(name = "document")
    public CompletableFuture<DocumentUploadResponse> uploadDocument(String correlationId, String idempotencyKey, DocumentUploadRequest request) {
        return webClient.post()
                .uri("/documents/upload")
                .header("X-Correlation-Id", correlationId)
                .header("X-Idempotency-Key", idempotencyKey)
                .bodyValue(request)
                .retrieve()
                .onStatus(status -> status.value() == 429 || status.value() == 503, this::retryableError)
                .onStatus(status -> status.isError(), this::nonRetryableError)
                .bodyToMono(DocumentUploadResponse.class)
                .toFuture();
    }

    private Mono<? extends Throwable> retryableError(ClientResponse response) {
        return Mono.just(new DocumentRetryableException("Retryable document error: " + response.statusCode()));
    }

    private Mono<? extends Throwable> nonRetryableError(ClientResponse response) {
        return Mono.just(new DocumentNonRetryableException("Non-retryable document error: " + response.statusCode()));
    }
}
