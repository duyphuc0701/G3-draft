package com.example.g3.client;

public class DocumentRetryableException extends RuntimeException {
    public DocumentRetryableException(String message) {
        super(message);
    }
}
