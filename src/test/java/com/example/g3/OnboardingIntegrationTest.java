package com.example.g3;

import com.example.g3.domain.OnboardingStatus;
import com.example.g3.dto.CreateSessionResponse;
import com.example.g3.dto.DocumentsRequest;
import com.example.g3.dto.PersonalDetailsRequest;
import com.example.g3.dto.StatusResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "cis.service.url=http://localhost:${wiremock.server.port}",
        "document.service.url=http://localhost:${wiremock.server.port}",
        "idv.service.url=http://localhost:${wiremock.server.port}",
        "risk.service.url=http://localhost:${wiremock.server.port}"
})
@AutoConfigureMockMvc
@AutoConfigureWireMock(port = 0)
public class OnboardingIntegrationTest {

    @org.springframework.boot.test.mock.mockito.MockBean
    private org.springframework.kafka.core.KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testHappyPathOnboardingFlow() throws Exception {
        // Mock downstream services
        stubFor(post(urlEqualTo("/documents/upload"))
                .withHeader("X-Correlation-Id", matching(".*"))
                .withHeader("X-Idempotency-Key", matching(".*"))
                .withHeader("Authorization", matching("Bearer .*"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"documentId\":\"doc_abc123\",\"storageLocation\":\"secure://path/to/object\"}")
                        .withStatus(201)));
        stubFor(post(urlPathMatching("/idv/verify/.*")).willReturn(aResponse().withStatus(200)));
        stubFor(post(urlEqualTo("/compliance/check"))
                .withHeader("X-Correlation-Id", matching(".*"))
                .withHeader("X-Idempotency-Key", matching(".*"))
                .withHeader("Authorization", matching("Bearer .*"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"amlStatus\":\"CLEAR\",\"pepStatus\":\"CLEAR\",\"reasonCodes\":[],\"checkedAt\":\"2026-03-20T03:21:34Z\"}")
                        .withStatus(200)));

        // 1. Start Onboarding
        String responseContent = mockMvc.perform(MockMvcRequestBuilders.post("/onboarding"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").exists())
                .andReturn().getResponse().getContentAsString();

        CreateSessionResponse sessionResponse = objectMapper.readValue(responseContent, CreateSessionResponse.class);
        String sessionId = sessionResponse.getSessionId().toString();

        // 2. Personal Details
        PersonalDetailsRequest personalDetailsRequest = new PersonalDetailsRequest();
        personalDetailsRequest.setFirstName("John");
        personalDetailsRequest.setLastName("Doe");
        personalDetailsRequest.setDob(java.time.LocalDate.of(1990, 1, 1));
        personalDetailsRequest.setEmail("john.doe@example.com");
        personalDetailsRequest.setContactPhone("+1234567890");

        mockMvc.perform(MockMvcRequestBuilders.put("/onboarding/" + sessionId + "/personal-details")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(personalDetailsRequest)))
                .andExpect(status().isOk());

        // 3. Document Upload
        DocumentsRequest documentsRequest = new DocumentsRequest();
        documentsRequest.setType("PASSPORT");
        documentsRequest.setFrontImage("front-base64");
        documentsRequest.setBackImage("back-base64");
        documentsRequest.setSelfieImage("selfie-base64");

        mockMvc.perform(MockMvcRequestBuilders.post("/onboarding/" + sessionId + "/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(documentsRequest)))
                .andExpect(status().isOk());

        // 4. Verify
        // Wiremock requires match on header for correlation id
        stubFor(post(urlPathMatching("/idv/verify"))
                .withHeader("X-Correlation-Id", matching(".*"))
                .withHeader("X-Idempotency-Key", matching(".*"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"verficationStatus\":\"VERIFIED\",\"reasonCodes\":[],\"riskScore\":0}")
                        .withStatus(200)));

        stubFor(post(urlEqualTo("/customers"))
                .withHeader("X-Correlation-Id", matching(".*"))
                .withHeader("X-Idempotency-Key", matching(".*"))
                .withHeader("Authorization", matching("Bearer .*"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"customerId\":\"cus_123456\",\"status\":\"CREATED\"}")
                        .withStatus(201)));

        mockMvc.perform(MockMvcRequestBuilders.post("/onboarding/" + sessionId + "/verify"))
                .andExpect(status().isAccepted());

        // 5. Check Status
        mockMvc.perform(MockMvcRequestBuilders.get("/onboarding/" + sessionId + "/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(OnboardingStatus.ACCOUNT_REQUESTED.name()));
    }
}
