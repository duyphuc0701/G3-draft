package com.example.g3.messaging;

import com.example.g3.domain.OnboardingSession;
import com.example.g3.domain.OnboardingStatus;
import com.example.g3.repository.OnboardingSessionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccountEventConsumerTest {

    private final OnboardingSessionRepository repository = mock(OnboardingSessionRepository.class);
    private final OnboardingEventProducer eventProducer = mock(OnboardingEventProducer.class);
    private final AccountEventConsumer consumer = new AccountEventConsumer(repository, eventProducer, new ObjectMapper());

    @Test
    void consumesAccountOpenedByEventTypeHeaderFromAccountEventsTopic() {
        UUID sessionId = UUID.randomUUID();
        OnboardingSession session = new OnboardingSession();
        session.setId(sessionId);
        session.setStatus(OnboardingStatus.ACCOUNT_REQUESTED);
        when(repository.findById(sessionId)).thenReturn(Optional.of(session));

        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "topic-account-events",
                0,
                0,
                sessionId.toString(),
                """
                        {"requestId":"%s","customerId":"cus_123456","accountId":"acc_123","status":"OPENED"}
                        """.formatted(sessionId)
        );
        record.headers().add("eventType", "account.opened".getBytes(StandardCharsets.UTF_8));

        consumer.consumeAccountEvent(record);

        assertThat(session.getStatus()).isEqualTo(OnboardingStatus.ACCOUNT_OPENED);
        assertThat(session.getAccountId()).isEqualTo("acc_123");
        verify(repository).save(session);
        verify(eventProducer).publishStatusChanged(session);
    }

    @Test
    void ignoresAccountRequestedEventOnSharedAccountEventsTopic() {
        UUID sessionId = UUID.randomUUID();
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "topic-account-events",
                0,
                0,
                sessionId.toString(),
                """
                        {"requestId":"%s","customerId":"cus_123456","productType":"PERSONAL_CURRENT"}
                        """.formatted(sessionId)
        );
        record.headers().add("eventType", "account.requested".getBytes(StandardCharsets.UTF_8));

        consumer.consumeAccountEvent(record);

        verify(repository, never()).findById(sessionId);
        verify(eventProducer, never()).publishStatusChanged(org.mockito.ArgumentMatchers.any());
    }
}
