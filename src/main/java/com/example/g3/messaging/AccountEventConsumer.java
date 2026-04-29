package com.example.g3.messaging;

import com.example.g3.domain.OnboardingSession;
import com.example.g3.domain.OnboardingStatus;
import com.example.g3.dto.AccountFailedEvent;
import com.example.g3.dto.AccountOpenedEvent;
import com.example.g3.repository.OnboardingSessionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AccountEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(AccountEventConsumer.class);
    private final OnboardingSessionRepository repository;
    private final OnboardingEventProducer eventProducer;
    private final ObjectMapper objectMapper;

    public AccountEventConsumer(OnboardingSessionRepository repository, OnboardingEventProducer eventProducer, ObjectMapper objectMapper) {
        this.repository = repository;
        this.eventProducer = eventProducer;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = {"account.opened", "account.failed", "account.requested.DLQ"}, groupId = "orchestrator-group")
    public void consumeAccountEvent(String message, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.info("Received event on topic {}: {}", topic, message);
        try {
            if ("account.opened".equals(topic)) {
                AccountOpenedEvent event = objectMapper.readValue(message, AccountOpenedEvent.class);
                UUID sessionId = UUID.fromString(event.getRequestId());
                repository.findById(sessionId).ifPresent(session -> {
                    if (session.getStatus() != OnboardingStatus.ACCOUNT_OPENED && session.getAccountId() == null) {
                        session.setAccountId(event.getAccountId());
                        session.setStatus(OnboardingStatus.ACCOUNT_OPENED);
                        repository.save(session);
                        eventProducer.publishStatusChanged(session);
                    } else {
                        log.info("Duplicate or already processed account.opened event for sessionId: {}", sessionId);
                    }
                });
            } else if ("account.failed".equals(topic)) {
                AccountFailedEvent event = objectMapper.readValue(message, AccountFailedEvent.class);
                UUID sessionId = UUID.fromString(event.getRequestId());
                repository.findById(sessionId).ifPresent(session -> {
                    if (session.getStatus() != OnboardingStatus.FAILED) {
                        session.setAccountFailureCode(event.getFailureCode());
                        session.setAccountFailureReason(event.getReason());
                        session.setStatus(OnboardingStatus.FAILED);
                        repository.save(session);
                        eventProducer.publishStatusChanged(session);
                    }
                });
            } else if ("account.requested.DLQ".equals(topic)) {
                // We expect the original account.requested payload, which has a requestId
                JsonNode jsonNode = objectMapper.readTree(message);
                if (jsonNode.has("requestId")) {
                    UUID sessionId = UUID.fromString(jsonNode.get("requestId").asText());
                    repository.findById(sessionId).ifPresent(session -> {
                        if (session.getStatus() != OnboardingStatus.FAILED) {
                            session.setAccountFailureCode("CORE_ADAPTER_UNAVAILABLE");
                            session.setAccountFailureReason("Core adapter failed to process account request after max retries");
                            session.setStatus(OnboardingStatus.FAILED);
                            repository.save(session);
                            eventProducer.publishStatusChanged(session);
                        }
                    });
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse or process Kafka message on topic {}", topic, e);
        }
    }
}
