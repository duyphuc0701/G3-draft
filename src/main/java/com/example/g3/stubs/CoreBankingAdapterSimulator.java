package com.example.g3.stubs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class CoreBankingAdapterSimulator {
    private static final Logger log = LoggerFactory.getLogger(CoreBankingAdapterSimulator.class);
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);
    private final Random random = new Random();

    public CoreBankingAdapterSimulator(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    // Listen to the topic. Note: Ensure your group ID is DIFFERENT from your Orchestrator's group ID
    @KafkaListener(topics = "topic-account-events", groupId = "core-banking-simulator-group")
    public void consumeAccountRequested(ConsumerRecord<String, String> record) {
        try {
            // 1. Extract the eventType from the Kafka Headers
            org.apache.kafka.common.header.Header eventTypeHeader = record.headers().lastHeader("eventType");
            
            if (eventTypeHeader == null) {
                log.warn("Simulator received a message with no eventType header. Ignoring.");
                return;
            }

            String eventType = new String(eventTypeHeader.value(), StandardCharsets.UTF_8);

            // 2. Filter strictly for "account.requested"
            if ("account.requested".equals(eventType)) {
                JsonNode payload = objectMapper.readTree(record.value());
                
                String customerId = payload.has("customerId") ? payload.get("customerId").asText() : "cus_123456";
                
                // Extract correlationId from the headers just like the Orchestrator sent it!
                org.apache.kafka.common.header.Header correlationIdHeader = record.headers().lastHeader("correlationId");
                String correlationId = correlationIdHeader != null 
                        ? new String(correlationIdHeader.value(), StandardCharsets.UTF_8) 
                        : UUID.randomUUID().toString();
                
                log.info("Simulator received {} for customer: {}. Processing...", eventType, customerId);

                // 3. Simulate the 1 to 10 second legacy core delay
                int delaySeconds = random.nextInt(10) + 1;

                scheduler.schedule(() -> produceAccountResult(customerId, correlationId), delaySeconds, TimeUnit.SECONDS);
            } else {
                // Silently ignore "account.opened" or "account.failed" so we don't create an infinite loop
                log.debug("Simulator ignoring event type: {}", eventType);
            }
            
        } catch (Exception e) {
            log.error("Simulator failed to parse message", e);
        }
    }

    private void produceAccountResult(String customerId, String correlationId) {
        try {
            // 1. Create the Payload
            ObjectNode responseEvent = objectMapper.createObjectNode();
            responseEvent.put("requestId", correlationId); 
            responseEvent.put("customerId", customerId);
            
            // Let's make it realistic: 80% chance of success, 20% chance of failure
            boolean isSuccess = random.nextInt(10) < 8;
            
            // Determine the explicit event type based on success/failure
            String eventTypeHeader = isSuccess ? "account.opened" : "account.failed";

            if (isSuccess) {
                responseEvent.put("accountId", "acc_" + random.nextInt(999999)); 
                responseEvent.put("status", "OPENED"); 
                responseEvent.put("openedAt", Instant.now().toString());
            } else {
                responseEvent.put("status", "FAILED"); 
                responseEvent.put("failureCode", "CORE_REJECTED");
                responseEvent.put("reason", "Simulated legacy core rejection");
            }

            String payload = objectMapper.writeValueAsString(responseEvent);

            // 2. Wrap it in a ProducerRecord so we can attach the HEADERS
            ProducerRecord<String, String> record = new ProducerRecord<>("topic-account-events", correlationId, payload);
            
            // THIS IS THE MISSING PIECE: Telling the Orchestrator what kind of event this is!
            record.headers().add("eventType", eventTypeHeader.getBytes(StandardCharsets.UTF_8));
            record.headers().add("correlationId", correlationId.getBytes(StandardCharsets.UTF_8));

            // 3. Send the primary event
            log.info("Simulator publishing event type [{}] with payload: {}", eventTypeHeader, payload);
            kafkaTemplate.send(record);

            // 4. Simulate the chaotic nature of the legacy adapter (Duplicate events)
            if (random.nextInt(10) > 7) { 
                scheduler.schedule(() -> {
                    log.warn("Simulator publishing DUPLICATE [{}] for {}", eventTypeHeader, customerId);
                    kafkaTemplate.send(record); // Send the exact same record with headers again
                }, 2, TimeUnit.SECONDS);
            }

        } catch (Exception e) {
            log.error("Simulator failed to send result", e);
        }
    }
}
