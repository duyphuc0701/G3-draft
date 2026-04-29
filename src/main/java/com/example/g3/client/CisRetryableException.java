package com.example.g3.client;

public class CisRetryableException extends RuntimeException {
    public CisRetryableException(String message) {
        super(message);
    }
}
