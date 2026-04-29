package com.example.g3.client;

public class RiskNonRetryableException extends RuntimeException {
    public RiskNonRetryableException(String message) {
        super(message);
    }
}
