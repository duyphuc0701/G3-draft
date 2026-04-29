package com.example.g3.dto;

import java.util.List;

public class IdvVerifyResponse {
    private String verficationStatus;
    private List<String> reasonCodes;
    private int riskScore;

    public String getVerficationStatus() { return verficationStatus; }
    public void setVerficationStatus(String verficationStatus) { this.verficationStatus = verficationStatus; }
    public List<String> getReasonCodes() { return reasonCodes; }
    public void setReasonCodes(List<String> reasonCodes) { this.reasonCodes = reasonCodes; }
    public int getRiskScore() { return riskScore; }
    public void setRiskScore(int riskScore) { this.riskScore = riskScore; }
}
