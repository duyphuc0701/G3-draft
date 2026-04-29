package com.example.g3.service;

import com.example.g3.client.CisClient;
import com.example.g3.client.DocumentClient;
import com.example.g3.client.IdvClient;
import com.example.g3.client.RiskClient;
import com.example.g3.domain.OnboardingSession;
import com.example.g3.domain.OnboardingStatus;
import com.example.g3.dto.DocumentsRequest;
import com.example.g3.dto.PersonalDetailsRequest;
import com.example.g3.messaging.OnboardingEventProducer;
import com.example.g3.repository.OnboardingSessionRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

import com.example.g3.messaging.AccountEventProducer;

@Service
public class OnboardingService {

    private final OnboardingSessionRepository repository;
    private final CisClient cisClient;
    private final DocumentClient documentClient;
    private final IdvClient idvClient;
    private final RiskClient riskClient;
    private final AccountEventProducer accountEventProducer;
    private final OnboardingEventProducer eventProducer;

    public OnboardingService(OnboardingSessionRepository repository, CisClient cisClient,
                             DocumentClient documentClient, IdvClient idvClient,
                             RiskClient riskClient, AccountEventProducer accountEventProducer,
                             OnboardingEventProducer eventProducer) {
        this.repository = repository;
        this.cisClient = cisClient;
        this.documentClient = documentClient;
        this.idvClient = idvClient;
        this.riskClient = riskClient;
        this.accountEventProducer = accountEventProducer;
        this.eventProducer = eventProducer;
    }

    public UUID startOnboarding() {
        OnboardingSession session = new OnboardingSession();
        repository.save(session);
        publishEvent(session);
        return session.getId();
    }

    public void updatePersonalDetails(UUID sessionId, PersonalDetailsRequest request) {
        OnboardingSession session = getSession(sessionId);
        session.setFirstName(request.getFirstName());
        session.setLastName(request.getLastName());
        session.setDob(request.getDob());
        session.setEmail(request.getEmail());
        session.setContactPhone(request.getContactPhone());
        
        session.setStatus(OnboardingStatus.DETAILS_CAPTURED);
        repository.save(session);
        publishEvent(session);
    }

    public void uploadDocuments(UUID sessionId, DocumentsRequest request) {
        OnboardingSession session = getSession(sessionId);
        session.setIdDocumentType(request.getType());
        session.setFrontImage(request.getFrontImage());
        session.setBackImage(request.getBackImage());
        session.setSelfieImage(request.getSelfieImage());
        
        // 1. ID_FRONT
        com.example.g3.dto.DocumentUploadRequest frontReq = new com.example.g3.dto.DocumentUploadRequest();
        frontReq.setDocumentType("ID_FRONT");
        frontReq.setFileContent(request.getFrontImage());
        frontReq.setCustomerRef(sessionId.toString());
        com.example.g3.dto.DocumentUploadResponse frontResp = documentClient.uploadDocument(sessionId.toString(), sessionId.toString() + "-ID_FRONT", frontReq).join();
        session.setFrontDocumentId(frontResp.getDocumentId());
        
        // 2. ID_BACK
        com.example.g3.dto.DocumentUploadRequest backReq = new com.example.g3.dto.DocumentUploadRequest();
        backReq.setDocumentType("ID_BACK");
        backReq.setFileContent(request.getBackImage());
        backReq.setCustomerRef(sessionId.toString());
        com.example.g3.dto.DocumentUploadResponse backResp = documentClient.uploadDocument(sessionId.toString(), sessionId.toString() + "-ID_BACK", backReq).join();
        session.setBackDocumentId(backResp.getDocumentId());
        
        // 3. SELFIE
        com.example.g3.dto.DocumentUploadRequest selfieReq = new com.example.g3.dto.DocumentUploadRequest();
        selfieReq.setDocumentType("SELFIE");
        selfieReq.setFileContent(request.getSelfieImage());
        selfieReq.setCustomerRef(sessionId.toString());
        com.example.g3.dto.DocumentUploadResponse selfieResp = documentClient.uploadDocument(sessionId.toString(), sessionId.toString() + "-SELFIE", selfieReq).join();
        session.setSelfieDocumentId(selfieResp.getDocumentId());
        
        session.setStatus(OnboardingStatus.DOCUMENTS_UPLOADED);
        repository.save(session);
        publishEvent(session);
    }

    public void verify(UUID sessionId) {
        OnboardingSession session = getSession(sessionId);
        
        // Initial state for verification
        session.setStatus(OnboardingStatus.VERIFICATION_PENDING);
        repository.save(session);
        publishEvent(session);

        try {
            // Synchronous calls as per architecture
            com.example.g3.dto.IdvVerifyRequest idvRequest = new com.example.g3.dto.IdvVerifyRequest();
            idvRequest.setFirstName(session.getFirstName());
            idvRequest.setLastName(session.getLastName());
            idvRequest.setDob(session.getDob());
            idvRequest.setSelfieImage(session.getSelfieImage());
            
            com.example.g3.dto.IdvVerifyRequest.IdDocument doc = new com.example.g3.dto.IdvVerifyRequest.IdDocument();
            doc.setType(session.getIdDocumentType());
            doc.setFrontImage(session.getFrontImage());
            doc.setBackImage(session.getBackImage());
            idvRequest.setIdDocument(doc);

            // Wait for CompletableFuture from TimeLimiter
            idvClient.triggerVerification(sessionId.toString(), sessionId.toString(), idvRequest).join();
            // Risk & Compliance Check
            com.example.g3.dto.RiskComplianceRequest riskRequest = new com.example.g3.dto.RiskComplianceRequest();
            riskRequest.setFirstName(session.getFirstName());
            riskRequest.setLastName(session.getLastName());
            riskRequest.setDob(session.getDob());
            
            com.example.g3.dto.RiskComplianceResponse riskResponse = riskClient.performComplianceCheck(sessionId.toString(), sessionId.toString(), riskRequest).join();
            
            session.setAmlStatus(riskResponse.getAmlStatus());
            session.setPepStatus(riskResponse.getPepStatus());
            session.setAmlReasonCodes(riskResponse.getReasonCodes());
            
            if ("REJECTED".equals(riskResponse.getAmlStatus()) || "REJECTED".equals(riskResponse.getPepStatus())) {
                session.setStatus(OnboardingStatus.FAILED);
                repository.save(session);
                publishEvent(session);
                return;
            }
            
            if ("REVIEW".equals(riskResponse.getAmlStatus()) || "REVIEW".equals(riskResponse.getPepStatus())) {
                session.setStatus(OnboardingStatus.PENDING_REVIEW);
                repository.save(session);
                publishEvent(session);
                return;
            }

            // Successfully Verified
            session.setStatus(OnboardingStatus.VERIFIED);
            repository.save(session);
            publishEvent(session);

            // Create Profile
            com.example.g3.dto.CisCustomerRequest cisRequest = new com.example.g3.dto.CisCustomerRequest();
            cisRequest.setFirstName(session.getFirstName());
            cisRequest.setLastName(session.getLastName());
            cisRequest.setDob(session.getDob());
            cisRequest.setContactEmail(session.getEmail());
            cisRequest.setContactPhone(session.getContactPhone());

            com.example.g3.dto.CisCustomerResponse cisResponse = cisClient.createCustomer(sessionId.toString(), sessionId.toString(), cisRequest);
            session.setCustomerId(cisResponse.getCustomerId());

            // Successfully Created CIS
            session.setStatus(OnboardingStatus.CIS_CREATED);
            repository.save(session);
            publishEvent(session);

            // Request Account Creation Async via Kafka
            accountEventProducer.publishAccountRequested(session, sessionId.toString());

            // Update state
            session.setStatus(OnboardingStatus.ACCOUNT_REQUESTED);
        } catch (Exception e) {
            session.setStatus(OnboardingStatus.FAILED);
        }
        
        repository.save(session);
        publishEvent(session);
    }

    public OnboardingStatus getStatus(UUID sessionId) {
        return getSession(sessionId).getStatus();
    }

    private OnboardingSession getSession(UUID sessionId) {
        return repository.findById(sessionId).orElseThrow(() -> new IllegalArgumentException("Session not found"));
    }

    private void publishEvent(OnboardingSession session) {
        eventProducer.publishStatusChanged(session);
    }
}
