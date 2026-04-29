package com.example.g3.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import java.util.UUID;
import java.time.LocalDate;

@Entity
public class OnboardingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    private OnboardingStatus status;

    private String firstName;
    private String lastName;
    private String email;
    private String contactPhone;
    private LocalDate dob;
    
    private String customerId;

    private String idDocumentType;
    @Column(length = 2000)
    private String frontImage;
    @Column(length = 2000)
    private String backImage;
    @Column(length = 2000)
    private String selfieImage;

    private String frontDocumentId;
    private String backDocumentId;
    private String selfieDocumentId;

    private String amlStatus;
    private String pepStatus;
    
    @ElementCollection
    private java.util.List<String> amlReasonCodes;

    private String accountId;
    private String accountFailureCode;
    private String accountFailureReason;

    public OnboardingSession() {
        this.status = OnboardingStatus.STARTED;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public OnboardingStatus getStatus() {
        return status;
    }

    public void setStatus(OnboardingStatus status) {
        this.status = status;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public LocalDate getDob() {
        return dob;
    }

    public void setDob(LocalDate dob) {
        this.dob = dob;
    }

    public String getIdDocumentType() {
        return idDocumentType;
    }

    public void setIdDocumentType(String idDocumentType) {
        this.idDocumentType = idDocumentType;
    }

    public String getFrontImage() {
        return frontImage;
    }

    public void setFrontImage(String frontImage) {
        this.frontImage = frontImage;
    }

    public String getBackImage() {
        return backImage;
    }

    public void setBackImage(String backImage) {
        this.backImage = backImage;
    }

    public String getSelfieImage() {
        return selfieImage;
    }

    public void setSelfieImage(String selfieImage) {
        this.selfieImage = selfieImage;
    }

    public String getFrontDocumentId() {
        return frontDocumentId;
    }

    public void setFrontDocumentId(String frontDocumentId) {
        this.frontDocumentId = frontDocumentId;
    }

    public String getBackDocumentId() {
        return backDocumentId;
    }

    public void setBackDocumentId(String backDocumentId) {
        this.backDocumentId = backDocumentId;
    }

    public String getSelfieDocumentId() {
        return selfieDocumentId;
    }

    public void setSelfieDocumentId(String selfieDocumentId) {
        this.selfieDocumentId = selfieDocumentId;
    }

    public String getAmlStatus() {
        return amlStatus;
    }

    public void setAmlStatus(String amlStatus) {
        this.amlStatus = amlStatus;
    }

    public String getPepStatus() {
        return pepStatus;
    }

    public void setPepStatus(String pepStatus) {
        this.pepStatus = pepStatus;
    }

    public java.util.List<String> getAmlReasonCodes() {
        return amlReasonCodes;
    }

    public void setAmlReasonCodes(java.util.List<String> amlReasonCodes) {
        this.amlReasonCodes = amlReasonCodes;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getAccountFailureCode() {
        return accountFailureCode;
    }

    public void setAccountFailureCode(String accountFailureCode) {
        this.accountFailureCode = accountFailureCode;
    }

    public String getAccountFailureReason() {
        return accountFailureReason;
    }

    public void setAccountFailureReason(String accountFailureReason) {
        this.accountFailureReason = accountFailureReason;
    }
}
