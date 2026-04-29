package com.example.g3.messaging;

import com.example.g3.domain.OnboardingSession;
import com.example.g3.dto.AccountRequestedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class AccountEventProducer {

    private static final Logger log = LoggerFactory.getLogger(AccountEventProducer.class);
    private static final String TOPIC = "account.requested";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public AccountEventProducer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publishAccountRequested(OnboardingSession session, String correlationId) {
        AccountRequestedEvent event = new AccountRequestedEvent();
        event.setRequestId(session.getId().toString());
        event.setCustomerId(session.getCustomerId());
        event.setProductType("PERSONAL_CURRENT");
        
        Map<String, String> metadata = new HashMap<>();
        metadata.put("channel", "MOBILE");
        metadata.put("referralCode", "ABCD123");
        event.setMetadata(metadata);

        try {
            String payload = objectMapper.writeValueAsString(event);
            
            ProducerRecord<String, String> record = new ProducerRecord<>(TOPIC, session.getCustomerId(), payload);
            record.headers().add("correlationId", correlationId.getBytes(StandardCharsets.UTF_8));
            record.headers().add("eventType", "account.requested".getBytes(StandardCharsets.UTF_8));
            record.headers().add("version", "1".getBytes(StandardCharsets.UTF_8));
            record.headers().add("occuredAt", Instant.now().toString().getBytes(StandardCharsets.UTF_8));
            
            kafkaTemplate.send(record);
            log.info("Published account.requested for sessionId: {} with customerId: {}", session.getId(), session.getCustomerId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize AccountRequestedEvent for sessionId: {}", session.getId(), e);
        }
    }
}
