package com.example.g3.client;

public class DocumentNonRetryableException extends RuntimeException {
    public DocumentNonRetryableException(String message) {
        super(message);
    }
}
