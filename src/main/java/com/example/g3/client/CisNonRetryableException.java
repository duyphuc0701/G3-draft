package com.example.g3.client;

public class CisNonRetryableException extends RuntimeException {
    public CisNonRetryableException(String message) {
        super(message);
    }
}
