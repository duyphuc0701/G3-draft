package com.example.g3.client;

public class RiskRetryableException extends RuntimeException {
    public RiskRetryableException(String message) {
        super(message);
    }
}
