package com.example.g3.dto;

import java.util.Map;

public class AccountRequestedEvent {
    private String requestId;
    private String customerId;
    private String productType;
    private Map<String, String> metadata;

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }

    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }
}
