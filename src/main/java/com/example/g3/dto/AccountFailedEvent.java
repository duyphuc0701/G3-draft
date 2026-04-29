package com.example.g3.dto;

public class AccountFailedEvent {
    private String requestId;
    private String customerId;
    private String failureCode;
    private String reason;

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getFailureCode() { return failureCode; }
    public void setFailureCode(String failureCode) { this.failureCode = failureCode; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
