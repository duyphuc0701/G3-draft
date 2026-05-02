package com.example.g3.messaging;

import com.example.g3.domain.OnboardingStatus;
import com.example.g3.dto.AccountFailedEvent;
import com.example.g3.dto.AccountOpenedEvent;
import com.example.g3.repository.OnboardingSessionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
public class AccountEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(AccountEventConsumer.class);
    private static final String ACCOUNT_EVENTS_TOPIC = "topic-account-events";
    private static final String ACCOUNT_OPENED = "account.opened";
    private static final String ACCOUNT_FAILED = "account.failed";

    private final OnboardingSessionRepository repository;
    private final OnboardingEventProducer eventProducer;
    private final ObjectMapper objectMapper;

    public AccountEventConsumer(OnboardingSessionRepository repository, OnboardingEventProducer eventProducer, ObjectMapper objectMapper) {
        this.repository = repository;
        this.eventProducer = eventProducer;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = ACCOUNT_EVENTS_TOPIC, groupId = "orchestrator-group")
    public void consumeAccountEvent(ConsumerRecord<String, String> record) {
        String eventType = getHeaderValue(record, "eventType");
        String message = record.value();

        if (eventType == null) {
            log.warn("Received account event without eventType header on topic {}. Ignoring.", record.topic());
            return;
        }

        log.info("Received {} on topic {}: {}", eventType, record.topic(), message);
        try {
            if (ACCOUNT_OPENED.equals(eventType)) {
                handleAccountOpened(message);
            } else if (ACCOUNT_FAILED.equals(eventType)) {
                handleAccountFailed(message);
            } else {
                log.debug("Ignoring account event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Failed to parse or process Kafka message with eventType {}", eventType, e);
        }
    }

    private void handleAccountOpened(String message) throws Exception {
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
    }

    private void handleAccountFailed(String message) throws Exception {
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
    }

    private String getHeaderValue(ConsumerRecord<String, String> record, String headerName) {
        Header header = record.headers().lastHeader(headerName);
        return header == null ? null : new String(header.value(), StandardCharsets.UTF_8);
    }
}
