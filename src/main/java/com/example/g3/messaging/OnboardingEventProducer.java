package com.example.g3.messaging;

import com.example.g3.domain.OnboardingSession;
import com.example.g3.domain.OnboardingStatus;
import com.example.g3.dto.OnboardingStatusEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.EnumSet;
import java.util.UUID;

@Component
public class OnboardingEventProducer {

    private static final Logger log = LoggerFactory.getLogger(OnboardingEventProducer.class);
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private static final String TOPIC = "onboarding.status.changed";

    private static final EnumSet<OnboardingStatus> NOTIFICATION_STATUSES = EnumSet.of(
            OnboardingStatus.STARTED,
            OnboardingStatus.VERIFIED,
            OnboardingStatus.CIS_CREATED,
            OnboardingStatus.ACCOUNT_REQUESTED,
            OnboardingStatus.ACCOUNT_OPENED,
            OnboardingStatus.FAILED,
            OnboardingStatus.PENDING_REVIEW
    );

    public OnboardingEventProducer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publishStatusChanged(OnboardingSession session) {
        if (!NOTIFICATION_STATUSES.contains(session.getStatus())) {
            log.debug("Status {} is not mapped for notification, skipping event", session.getStatus());
            return;
        }

        OnboardingStatusEvent event = new OnboardingStatusEvent();
        event.setOnboardingId(session.getId().toString());
        event.setCustomerId(session.getCustomerId());
        event.setStatus(session.getStatus().name());
        event.setTimestamp(Instant.now());

        if (session.getEmail() != null || session.getContactPhone() != null) {
            OnboardingStatusEvent.CustomerContact contact = new OnboardingStatusEvent.CustomerContact();
            contact.setEmail(session.getEmail());
            contact.setPhone(session.getContactPhone());
            event.setCustomerContact(contact);
        }

        try {
            String payload = objectMapper.writeValueAsString(event);

            ProducerRecord<String, String> record = new ProducerRecord<>(TOPIC, session.getId().toString(), payload);
            String correlationId = UUID.randomUUID().toString(); // Or get from MDC/context if available
            record.headers().add("correlationId", correlationId.getBytes(StandardCharsets.UTF_8));
            record.headers().add("eventType", "onboarding.status.changed".getBytes(StandardCharsets.UTF_8));
            record.headers().add("version", "1".getBytes(StandardCharsets.UTF_8));
            record.headers().add("occuredAt", Instant.now().toString().getBytes(StandardCharsets.UTF_8));

            kafkaTemplate.send(record);
            log.info("Published onboarding.status.changed for onboardingId: {} to status: {}", session.getId(), session.getStatus());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize OnboardingStatusEvent for onboardingId: {}", session.getId(), e);
        }
    }
}
