package com.example.g3.dto;

public class RiskErrorResponse {
    private String type;
    private String title;
    private int status;
    private String correlationId;
    private String error_code;
    private String detail;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public String getError_code() { return error_code; }
    public void setError_code(String error_code) { this.error_code = error_code; }

    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
}
