package com.example.demo.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message, String txnRef) {
        super(message);
    }
    public ResourceNotFoundException(String message, Long id) {
        super(message + id);
    }
}
