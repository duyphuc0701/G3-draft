package com.example.g3.dto;

import java.time.Instant;
import java.util.List;

public class RiskComplianceResponse {
    private String amlStatus;
    private String pepStatus;
    private List<String> reasonCodes;
    private Instant checkedAt;

    public String getAmlStatus() { return amlStatus; }
    public void setAmlStatus(String amlStatus) { this.amlStatus = amlStatus; }

    public String getPepStatus() { return pepStatus; }
    public void setPepStatus(String pepStatus) { this.pepStatus = pepStatus; }

    public List<String> getReasonCodes() { return reasonCodes; }
    public void setReasonCodes(List<String> reasonCodes) { this.reasonCodes = reasonCodes; }

    public Instant getCheckedAt() { return checkedAt; }
    public void setCheckedAt(Instant checkedAt) { this.checkedAt = checkedAt; }
}
